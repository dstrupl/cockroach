package cz.solutions.cockroach

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import kotlin.math.abs

object DividentReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateDividendReport(
        dividendRecordList: List<DividendRecord>,
        taxRecordList: List<TaxRecord>,
        taxReversalRecordList: List<TaxReversalRecord>,
        interval: DateInterval,
        exchangeRateProvider: ExchangeRateProvider
    ): DividendReport {

        val dividendsInInterval = dividendRecordList.filter { interval.contains(it.date) }
        val taxesInInterval = taxRecordList.filter { interval.contains(it.date) }
        val reversalsInInterval = taxReversalRecordList.filter { interval.contains(it.date) }

        val dividendsByCurrency = dividendsInInterval.groupBy { it.currency }
        val taxesByCurrency = taxesInInterval.groupBy { it.currency }
        val reversalsByCurrency = reversalsInInterval.groupBy { it.currency }

        val nonCzkCurrencies = (dividendsByCurrency.keys + taxesByCurrency.keys + reversalsByCurrency.keys)
            .filter { it != Currency.CZK }
            .sortedBy { it.name }

        val sections = nonCzkCurrencies.map { currency ->
            buildCurrencySection(
                currency,
                dividendsByCurrency[currency].orEmpty(),
                taxesByCurrency[currency].orEmpty(),
                reversalsByCurrency[currency].orEmpty(),
                exchangeRateProvider
            )
        }

        val czkSection = if (Currency.CZK in dividendsByCurrency.keys || Currency.CZK in taxesByCurrency.keys || Currency.CZK in reversalsByCurrency.keys) {
            buildCzkSection(
                dividendsByCurrency[Currency.CZK].orEmpty(),
                taxesByCurrency[Currency.CZK].orEmpty(),
                reversalsByCurrency[Currency.CZK].orEmpty()
            )
        } else null

        return DividendReport(sections, czkSection)
    }

    private fun buildCurrencySection(
        currency: Currency,
        dividendRecords: List<DividendRecord>,
        taxRecords: List<TaxRecord>,
        reversalRecords: List<TaxReversalRecord>,
        exchangeRateProvider: ExchangeRateProvider
    ): CurrencyDividendSection {
        val sortedDividends = dividendRecords.sortedBy { it.date }
        val taxesByDate = taxRecords.groupBy({ it.date }) { it }.mapValues { it.value.toMutableList() }
        val printable = mutableListOf<PrintableDividend>()
        var totalBrutto = 0.0
        var totalTax = 0.0
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for (dividendRecord in sortedDividends) {
            val exchange = exchangeRateProvider.rateAt(dividendRecord.date, currency)
            val taxCandidates = taxesByDate[dividendRecord.date]
            val taxRecord = taxCandidates?.minByOrNull { abs(abs(it.amount) - abs(dividendRecord.amount) * 0.15) } //if there were more taxes on the same day, we take the one closest to 15% of dividend amount, because that's the most likely correct one
                ?: error(missingTaxMessage(dividendRecord, currency))
            taxCandidates.remove(taxRecord)
            totalBrutto += dividendRecord.amount
            totalTax += taxRecord.amount
            totalBruttoCrown += dividendRecord.amount * exchange
            totalTaxCrown += taxRecord.amount * exchange

            printable.add(
                PrintableDividend(
                    dividendRecord.symbol,
                    dividendRecord.broker,
                    DATE_FORMATTER.print(dividendRecord.date),
                    FormatingHelper.formatDouble(dividendRecord.amount),
                    FormatingHelper.formatExchangeRate(exchange),
                    FormatingHelper.formatDouble(taxRecord.amount),
                    FormatingHelper.formatDouble(exchange * dividendRecord.amount),
                    FormatingHelper.formatDouble(exchange * taxRecord.amount)
                )
            )
        }

        val totalTaxReversal = reversalRecords.sumOf { it.amount }
        val totalTaxReversalCrown = reversalRecords.sumOf { it.amount * exchangeRateProvider.rateAt(it.date, currency) }

        return CurrencyDividendSection(currency, printable, totalBrutto, totalTax, totalBruttoCrown, totalTaxCrown, totalTaxReversal, totalTaxReversalCrown)
    }

    private fun buildCzkSection(
        dividendRecords: List<DividendRecord>,
        taxRecords: List<TaxRecord>,
        reversalRecords: List<TaxReversalRecord>
    ): CzkDividendSection {
        val sortedDividends = dividendRecords.sortedBy { it.date }
        val taxesByDate = taxRecords.groupBy({ it.date }) { it }.mapValues { it.value.toMutableList() }
        val printable = mutableListOf<PrintableCzkDividend>()
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for (dividendRecord in sortedDividends) {
            val taxCandidates = taxesByDate[dividendRecord.date]
            val taxRecord = taxCandidates?.minByOrNull { abs(abs(it.amount) - abs(dividendRecord.amount) * 0.15) }
                ?: error(missingTaxMessage(dividendRecord, Currency.CZK))
            taxCandidates.remove(taxRecord)
            totalBruttoCrown += dividendRecord.amount
            totalTaxCrown += taxRecord.amount
            printable.add(
                PrintableCzkDividend(
                    dividendRecord.symbol,
                    dividendRecord.broker,
                    DATE_FORMATTER.print(dividendRecord.date),
                    FormatingHelper.formatDouble(dividendRecord.amount),
                    FormatingHelper.formatDouble(taxRecord.amount)
                )
            )
        }

        val totalTaxReversalCrown = reversalRecords.sumOf { it.amount }
        return CzkDividendSection(printable, totalBruttoCrown, totalTaxCrown, totalTaxReversalCrown)
    }

    private fun missingTaxMessage(dividend: DividendRecord, currency: Currency): String {
        val symbol = dividend.symbol
        val broker = dividend.broker
        return "No matching tax record found for dividend on ${DATE_FORMATTER.print(dividend.date)} " +
                "(broker=$broker, symbol=$symbol, amount=${FormatingHelper.formatDouble(dividend.amount)} ${currency.name}). " +
                "If withholding tax is genuinely 0%, add an explicit TaxRecord with amount=0.0 on the same date in the parser; " +
                "otherwise verify that the broker statement contains the corresponding tax row and that its date matches the dividend date."
    }
}