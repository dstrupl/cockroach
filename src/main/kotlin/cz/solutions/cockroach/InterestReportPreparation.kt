package cz.solutions.cockroach

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object InterestReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateInterestReport(
        interestRecordList: List<InterestRecord>,
        interval: DateInterval,
        exchangeRateProvider: ExchangeRateProvider
    ): InterestReport {

        val interestsInInterval = interestRecordList.filter { interval.contains(it.date) }
        val interestsByCurrency = interestsInInterval.groupBy { it.currency }

        val nonCzkCurrencies = interestsByCurrency.keys
            .filter { it != Currency.CZK }
            .sortedBy { it.name }

        val sections = nonCzkCurrencies.map { currency ->
            buildCurrencySection(currency, interestsByCurrency.getValue(currency), exchangeRateProvider)
        }

        val czkSection = interestsByCurrency[Currency.CZK]?.let { buildCzkSection(it) }

        return InterestReport(sections, czkSection)
    }

    private fun buildCurrencySection(
        currency: Currency,
        interestRecords: List<InterestRecord>,
        exchangeRateProvider: ExchangeRateProvider
    ): CurrencyInterestSection {
        val sortedInterests = interestRecords.sortedBy { it.date }
        val printable = mutableListOf<PrintableInterest>()
        var totalBrutto = 0.0
        var totalBruttoCrown = 0.0

        for (interestRecord in sortedInterests) {
            val exchange = exchangeRateProvider.rateAt(interestRecord.date, currency)
            totalBrutto += interestRecord.amount
            totalBruttoCrown += interestRecord.amount * exchange

            printable.add(
                PrintableInterest(
                    DATE_FORMATTER.print(interestRecord.date),
                    FormatingHelper.formatDouble(interestRecord.amount),
                    FormatingHelper.formatExchangeRate(exchange),
                    FormatingHelper.formatDouble(exchange * interestRecord.amount)
                )
            )
        }

        return CurrencyInterestSection(currency, printable, totalBrutto, totalBruttoCrown)
    }

    private fun buildCzkSection(interestRecords: List<InterestRecord>): CzkInterestSection {
        val sortedInterests = interestRecords.sortedBy { it.date }
        val printable = mutableListOf<PrintableCzkInterest>()
        var totalBruttoCrown = 0.0

        for (interestRecord in sortedInterests) {
            totalBruttoCrown += interestRecord.amount
            printable.add(
                PrintableCzkInterest(
                    DATE_FORMATTER.print(interestRecord.date),
                    FormatingHelper.formatDouble(interestRecord.amount)
                )
            )
        }
        return CzkInterestSection(printable, totalBruttoCrown)
    }
}
