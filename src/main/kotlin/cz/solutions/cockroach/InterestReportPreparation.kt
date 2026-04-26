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
            .map { withResolvedCountry(it) }
        val grouped = interestsInInterval.groupBy { it.country to it.currency }

        val sections = grouped.entries
            .filter { (key, _) -> key.second != Currency.CZK }
            .sortedWith(compareBy({ it.key.first }, { it.key.second.name }))
            .map { (key, records) -> buildCurrencySection(key.first, key.second, records, exchangeRateProvider) }

        val czkSections = grouped.entries
            .filter { (key, _) -> key.second == Currency.CZK }
            .sortedBy { it.key.first }
            .map { (key, records) -> buildCzkSection(key.first, records) }

        val countryTotals = buildCountryTotals(sections, czkSections)

        return InterestReport(sections, czkSections, countryTotals)
    }

    /** Backfills a default country code so old test fixtures and unattributed records still group sanely. */
    private fun withResolvedCountry(record: InterestRecord): InterestRecord {
        if (record.country.isNotBlank()) return record
        val fallback = if (record.currency == Currency.CZK) "CZ" else "??"
        return record.copy(country = fallback)
    }

    private fun buildCountryTotals(
        sections: List<CurrencyInterestSection>,
        czkSections: List<CzkInterestSection>,
    ): List<CountryInterestTotal> {
        val accumulator = linkedMapOf<String, Pair<Double, Double>>()
        for (section in sections) {
            val (b, t) = accumulator[section.country] ?: (0.0 to 0.0)
            accumulator[section.country] = (b + section.totalBruttoCrown) to (t + section.totalTaxCrown)
        }
        for (section in czkSections) {
            val (b, t) = accumulator[section.country] ?: (0.0 to 0.0)
            accumulator[section.country] = (b + section.totalBruttoCrown) to (t + section.totalTaxCrown)
        }
        return accumulator.entries
            .sortedBy { it.key }
            .map { CountryInterestTotal(it.key, it.value.first, it.value.second) }
    }

    private fun buildCurrencySection(
        country: String,
        currency: Currency,
        interestRecords: List<InterestRecord>,
        exchangeRateProvider: ExchangeRateProvider
    ): CurrencyInterestSection {
        val sortedInterests = interestRecords.sortedBy { it.date }
        val printable = mutableListOf<PrintableInterest>()
        var totalBrutto = 0.0
        var totalTax = 0.0
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for (interestRecord in sortedInterests) {
            val exchange = exchangeRateProvider.rateAt(interestRecord.date, currency)
            val taxCrown = interestRecord.tax * exchange
            totalBrutto += interestRecord.amount
            totalTax += interestRecord.tax
            totalBruttoCrown += interestRecord.amount * exchange
            totalTaxCrown += taxCrown

            printable.add(
                PrintableInterest(
                    interestRecord.product,
                    interestRecord.broker,
                    DATE_FORMATTER.print(interestRecord.date),
                    FormatingHelper.formatDouble(interestRecord.amount),
                    FormatingHelper.formatDouble(interestRecord.tax),
                    FormatingHelper.formatExchangeRate(exchange),
                    FormatingHelper.formatDouble(exchange * interestRecord.amount),
                    FormatingHelper.formatDouble(taxCrown),
                )
            )
        }

        return CurrencyInterestSection(country, currency, printable, totalBrutto, totalTax, totalBruttoCrown, totalTaxCrown)
    }

    private fun buildCzkSection(country: String, interestRecords: List<InterestRecord>): CzkInterestSection {
        val sortedInterests = interestRecords.sortedBy { it.date }
        val printable = mutableListOf<PrintableCzkInterest>()
        var totalBruttoCrown = 0.0
        var totalTaxCrown = 0.0

        for (interestRecord in sortedInterests) {
            totalBruttoCrown += interestRecord.amount
            totalTaxCrown += interestRecord.tax
            printable.add(
                PrintableCzkInterest(
                    interestRecord.product,
                    interestRecord.broker,
                    DATE_FORMATTER.print(interestRecord.date),
                    FormatingHelper.formatDouble(interestRecord.amount),
                    FormatingHelper.formatDouble(interestRecord.tax),
                )
            )
        }
        return CzkInterestSection(country, printable, totalBruttoCrown, totalTaxCrown)
    }
}
