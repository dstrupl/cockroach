package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object DividentReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    /** Pairing key for a dividend payment: same broker, same issuer, same pay date.
     *  Multiple tax rows sharing this key (e.g. Schwab adjustments) are summed before the
     *  pairing; multiple dividend rows sharing this key are aggregated into a single
     *  printable line. Currency is implied by the section being built. */
    private data class PayKey(val broker: String, val symbol: String, val date: LocalDate) : Comparable<PayKey> {
        override fun compareTo(other: PayKey): Int = ORDER.compare(this, other)
        companion object { private val ORDER = compareBy<PayKey>({ it.date }, { it.symbol }, { it.broker }) }
    }

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

        // Czech-source vs. foreign split is driven by the issuer's country (ISIN prefix), not the
        // payment currency. Czech-source dividends are subject to final withholding (§ 36 ZDP) and
        // reported separately; foreign dividends go to Příloha č. 3.
        val czDividends = dividendsInInterval.filter { it.country == "CZ" }
        val foreignDividends = dividendsInInterval.filter { it.country != "CZ" }

        // Sanity check: a CZ-source dividend paid in a non-CZK currency would land in a per-currency
        // foreign section but is conceptually Czech-source. Czech issuers virtually always pay in CZK
        // — flag the unusual case rather than guess how to convert.
        foreignDividends.firstOrNull { it.currency == Currency.CZK }?.let {
            error("Foreign-source dividend (country=${it.country}) paid in CZK is not supported by the current report layout " +
                    "(broker=${it.broker}, symbol=${it.symbol}, date=${DATE_FORMATTER.print(it.date)}). " +
                    "If this is genuine, extend DividentReportPreparation to handle a CZK foreign-currency section.")
        }

        val foreignDividendsByCurrency = foreignDividends.groupBy { it.currency }
        val taxesByCurrency = taxesInInterval.groupBy { it.currency }
        val reversalsByCurrency = reversalsInInterval.groupBy { it.currency }

        // Foreign per-currency sections cover every non-CZK currency that has any activity. CZK taxes
        // and reversals are routed to the Czech-source section below alongside CZ-country dividends.
        val nonCzkCurrencies = (foreignDividendsByCurrency.keys + taxesByCurrency.keys + reversalsByCurrency.keys)
            .filter { it != Currency.CZK }
            .sortedBy { it.name }

        val sections = nonCzkCurrencies.map { currency ->
            buildCurrencySection(
                currency,
                foreignDividendsByCurrency[currency].orEmpty(),
                taxesByCurrency[currency].orEmpty(),
                reversalsByCurrency[currency].orEmpty(),
                exchangeRateProvider
            )
        }

        val czkTaxes = taxesByCurrency[Currency.CZK].orEmpty()
        val czkReversals = reversalsByCurrency[Currency.CZK].orEmpty()
        val czkSection = if (czDividends.isNotEmpty() || czkTaxes.isNotEmpty() || czkReversals.isNotEmpty()) {
            buildCzkSection(czDividends, czkTaxes, czkReversals)
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
        val dividendsByKey = dividendRecords.groupBy { PayKey(it.broker, it.symbol, it.date) }
        val taxesByKey = taxRecords.groupBy { PayKey(it.broker, it.symbol, it.date) }

        verifyAllTaxesMatched(taxesByKey, dividendsByKey, currency)

        val printable = mutableListOf<PrintableDividend>()
        var totalBrutto = 0.0
        var totalTax = 0.0
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for ((key, dividendRecords) in dividendsByKey.toSortedMap()) {
            val taxes = taxesByKey[key] ?: error(missingTaxMessage(key, dividendRecords, currency))
            val exchange = exchangeRateProvider.rateAt(key.date, currency)
            val divAmount = dividendRecords.sumOf { it.amount }
            val taxAmount = taxes.sumOf { it.amount }
            totalBrutto += divAmount
            totalTax += taxAmount
            totalBruttoCrown += divAmount * exchange
            totalTaxCrown += taxAmount * exchange

            printable.add(
                PrintableDividend(
                    key.symbol,
                    key.broker,
                    DATE_FORMATTER.print(key.date),
                    FormatingHelper.formatDouble(divAmount),
                    FormatingHelper.formatExchangeRate(exchange),
                    FormatingHelper.formatDouble(taxAmount),
                    FormatingHelper.formatDouble(exchange * divAmount),
                    FormatingHelper.formatDouble(exchange * taxAmount)
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
        val dividendsByKey = dividendRecords.groupBy { PayKey(it.broker, it.symbol, it.date) }
        val taxesByKey = taxRecords.groupBy { PayKey(it.broker, it.symbol, it.date) }

        verifyAllTaxesMatched(taxesByKey, dividendsByKey, Currency.CZK)

        val printable = mutableListOf<PrintableCzkDividend>()
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for ((key, divs) in dividendsByKey.toSortedMap()) {
            val taxes = taxesByKey[key] ?: error(missingTaxMessage(key, divs, Currency.CZK))
            val divAmount = divs.sumOf { it.amount }
            val taxAmount = taxes.sumOf { it.amount }
            totalBruttoCrown += divAmount
            totalTaxCrown += taxAmount
            printable.add(
                PrintableCzkDividend(
                    key.symbol,
                    key.broker,
                    DATE_FORMATTER.print(key.date),
                    FormatingHelper.formatDouble(divAmount),
                    FormatingHelper.formatDouble(taxAmount)
                )
            )
        }

        val totalTaxReversalCrown = reversalRecords.sumOf { it.amount }
        return CzkDividendSection(printable, totalBruttoCrown, totalTaxCrown, totalTaxReversalCrown)
    }

    private fun verifyAllTaxesMatched(
        taxesByKey: Map<PayKey, List<TaxRecord>>,
        dividendsByKey: Map<PayKey, List<DividendRecord>>,
        currency: Currency,
    ) {
        val orphaned = taxesByKey.keys - dividendsByKey.keys
        check(orphaned.isEmpty()) {
            val sample = orphaned.min()
            val totalAmount = taxesByKey.getValue(sample).sumOf { it.amount }
            "Tax record without matching dividend in ${currency.name} section " +
                    "(broker=${sample.broker}, symbol=${sample.symbol}, date=${DATE_FORMATTER.print(sample.date)}, " +
                    "amount=${FormatingHelper.formatDouble(totalAmount)}). " +
                    "${orphaned.size - 1} other unmatched tax key(s). " +
                    "Verify the broker statement: every withholding row must be paired with a dividend row " +
                    "carrying the same broker/symbol/date — fix the parser or the input data."
        }
    }

    private fun missingTaxMessage(key: PayKey, dividendRecords: List<DividendRecord>, currency: Currency): String {
        val divAmount = dividendRecords.sumOf { it.amount }
        return "No matching tax record found for dividend on ${DATE_FORMATTER.print(key.date)} " +
                "(broker=${key.broker}, symbol=${key.symbol}, amount=${FormatingHelper.formatDouble(divAmount)} ${currency.name}). " +
                "If withholding tax is genuinely 0%, add an explicit TaxRecord with amount=0.0 on the same date in the parser; " +
                "otherwise verify that the broker statement contains the corresponding tax row and that its broker/symbol/date match the dividend."
    }
}