package cz.solutions.cockroach

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object DividentReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateDividendReport(
        dividendRecordList: List<DividendRecord>,
        taxRecordList: List<TaxRecord>,
        taxReversalRecordList: List<TaxReversalRecord>,
        interval: DateInterval,
        exchangeRateProvider: ExchangeRateProvider
    ): DividendReport {

        val dividendRecords = dividendRecordList
            .filter { interval.contains(it.date) }
            .sortedBy { it.date }

        val taxRecords = taxRecordList
            .filter { interval.contains(it.date) }
            .associateBy { it.date }

        val taxReversalRecords = taxReversalRecordList
            .filter { interval.contains(it.date) }
            .sortedBy { it.date }

        val printableDividendList = mutableListOf<PrintableDividend>()

        var totalBruttoDollar = 0.0
        var totalTaxDollar = 0.0
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for (dividendRecord in dividendRecords) {
            val exchange = exchangeRateProvider.rateAt(dividendRecord.date)
            val taxRecord = taxRecords[dividendRecord.date]

            if (taxRecord != null) {
                totalBruttoDollar += dividendRecord.amount
                totalTaxDollar += taxRecord.amount
                totalBruttoCrown += dividendRecord.amount * exchange
                totalTaxCrown += taxRecord.amount * exchange

                printableDividendList.add(
                    PrintableDividend(
                        DATE_FORMATTER.print(dividendRecord.date),
                        FormatingHelper.formatDouble(dividendRecord.amount),
                        FormatingHelper.formatExchangeRate(exchange),
                        FormatingHelper.formatDouble(taxRecord.amount),
                        FormatingHelper.formatDouble(exchange * dividendRecord.amount),
                        FormatingHelper.formatDouble(exchange * taxRecord.amount)
                    )
                )
            }
        }

        val totalTaxReversalDollar = taxReversalRecords.sumOf { it.amount }
        val totalTaxReversalCrown = taxReversalRecords.sumOf { it.amount * exchangeRateProvider.rateAt(it.date) }

        return DividendReport(
            printableDividendList,
            totalBruttoDollar,
            totalTaxDollar,
            totalBruttoCrown,
            totalTaxCrown,
            totalTaxReversalDollar,
            totalTaxReversalCrown
        )
    }
}