package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.Offset
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class DividentReportPreparationTest {

    private val fixedRate = ExchangeRateProvider { _, _ -> 25.0 }
    private val year2025 = DateInterval.year(2025)

    @Test
    fun taxRecordIsNotReusedWhenMultipleDividendsOnSameDate() {
        // Schwab: large dividend with 30% withholding
        // E-Trade: small dividend with 15% withholding
        // Both on the same date - simulates the real bug
        val dividends = listOf(
            dividendRecord(LocalDate(2025, 10, 22), 1426.80),  // Schwab
            dividendRecord(LocalDate(2025, 10, 22), 59.04)     // E-Trade
        )
        val taxes = listOf(
            TaxRecord(LocalDate(2025, 10, 22), -428.04),  // Schwab (30% of 1426.80)
            TaxRecord(LocalDate(2025, 10, 22), -8.86)     // E-Trade (15% of 59.04)
        )

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }

        // Both dividends should be matched
        assertThat(usd.printableDividendList).hasSize(2)

        // The total tax should include BOTH tax records, not -8.86 twice
        assertThat(usd.totalTax).isCloseTo(-436.90, Offset.offset(0.01))

        // Each tax should be used exactly once
        assertThat(usd.totalTaxCrown).isCloseTo(-436.90 * 25.0, Offset.offset(0.1))
    }

    @Test
    fun singleDividendWithSingleTaxOnSameDate() {
        val dividends = listOf(dividendRecord(LocalDate(2025, 1, 22), 1000.0))
        val taxes = listOf(TaxRecord(LocalDate(2025, 1, 22), -150.0))

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }
        assertThat(usd.printableDividendList).hasSize(1)
        assertThat(usd.totalTax).isCloseTo(-150.0, Offset.offset(0.01))
    }

    @Test
    fun dividendWithoutMatchingTaxRecordFailsWithDescriptiveMessage() {
        // A dividend without a tax row almost always means a parser bug or a missed tax row in the
        // broker statement. Force the user to investigate rather than silently under-reporting.
        val dividends = listOf(
            DividendRecord(LocalDate(2025, 6, 15), 500.0, symbol = "ACME", broker = "Schwab", country = "US")
        )

        assertThatThrownBy {
            DividentReportPreparation.generateDividendReport(
                dividends, emptyList(), emptyList(), year2025, fixedRate
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("No matching tax record")
            .hasMessageContaining("15.06.2025")
            .hasMessageContaining("broker=Schwab")
            .hasMessageContaining("symbol=ACME")
            .hasMessageContaining("USD")
            .hasMessageContaining("amount=0.0 on the same date")
    }

    @Test
    fun dividendsOnDifferentDatesMatchCorrectTax() {
        val dividends = listOf(
            dividendRecord(LocalDate(2025, 1, 22), 1000.0),
            dividendRecord(LocalDate(2025, 4, 23), 1200.0)
        )
        val taxes = listOf(
            TaxRecord(LocalDate(2025, 1, 22), -150.0),
            TaxRecord(LocalDate(2025, 4, 23), -180.0)
        )

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }
        assertThat(usd.printableDividendList).hasSize(2)
        assertThat(usd.totalTax).isCloseTo(-330.0, Offset.offset(0.01))
    }
}
