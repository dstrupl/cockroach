package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class InterestReportPreparationTest {

    private val fixedRate = ExchangeRateProvider { _, currency ->
        when (currency) {
            Currency.USD -> 20.0
            Currency.EUR -> 25.0
            else -> 1.0
        }
    }

    @Test
    fun aggregatesInterestPerCurrencyAndAppliesExchangeRate() {
        val records = listOf(
            InterestRecord(LocalDate(2025, 3, 15), 10.0, Currency.USD),
            InterestRecord(LocalDate(2025, 6, 20), 5.0, Currency.USD),
            InterestRecord(LocalDate(2025, 9, 10), 4.0, Currency.EUR),
        )

        val report = InterestReportPreparation.generateInterestReport(
            records, DateInterval.year(2025), fixedRate
        )

        assertThat(report.sections).hasSize(2)
        assertThat(report.czkSection).isNull()

        val usd = report.sections.first { it.currency == Currency.USD }
        assertThat(usd.totalBrutto).isCloseTo(15.0, offset(0.0001))
        assertThat(usd.totalBruttoCrown).isCloseTo(300.0, offset(0.0001))
        assertThat(usd.printableInterestList).hasSize(2)

        val eur = report.sections.first { it.currency == Currency.EUR }
        assertThat(eur.totalBrutto).isCloseTo(4.0, offset(0.0001))
        assertThat(eur.totalBruttoCrown).isCloseTo(100.0, offset(0.0001))

        assertThat(report.totalNonCzkBruttoCrown).isCloseTo(400.0, offset(0.0001))
        assertThat(report.totalBruttoCrown).isCloseTo(400.0, offset(0.0001))
    }

    @Test
    fun filtersRecordsOutsideOfInterval() {
        val records = listOf(
            InterestRecord(LocalDate(2024, 12, 31), 100.0, Currency.USD),
            InterestRecord(LocalDate(2025, 1, 1), 1.0, Currency.USD),
            InterestRecord(LocalDate(2025, 12, 31), 2.0, Currency.USD),
            InterestRecord(LocalDate(2026, 1, 1), 100.0, Currency.USD),
        )

        val report = InterestReportPreparation.generateInterestReport(
            records, DateInterval.year(2025), fixedRate
        )

        assertThat(report.sections).hasSize(1)
        assertThat(report.sections[0].totalBrutto).isCloseTo(3.0, offset(0.0001))
        assertThat(report.sections[0].totalBruttoCrown).isCloseTo(60.0, offset(0.0001))
    }

    @Test
    fun keepsCzkInterestInDedicatedSection() {
        val records = listOf(
            InterestRecord(LocalDate(2025, 5, 5), 1234.50, Currency.CZK),
            InterestRecord(LocalDate(2025, 7, 7), 100.0, Currency.USD),
        )

        val report = InterestReportPreparation.generateInterestReport(
            records, DateInterval.year(2025), fixedRate
        )

        assertThat(report.sections).hasSize(1)
        assertThat(report.sections[0].currency).isEqualTo(Currency.USD)
        assertThat(report.czkSection).isNotNull
        assertThat(report.czkSection!!.totalBruttoCrown).isCloseTo(1234.50, offset(0.0001))
        assertThat(report.totalBruttoCrown).isCloseTo(1234.50 + 2000.0, offset(0.0001))
    }

    @Test
    fun emptyInputProducesEmptyReport() {
        val report = InterestReportPreparation.generateInterestReport(
            emptyList(), DateInterval.year(2025), fixedRate
        )
        assertThat(report.sections).isEmpty()
        assertThat(report.czkSection).isNull()
        assertThat(report.totalBruttoCrown).isEqualTo(0.0)
    }
}
