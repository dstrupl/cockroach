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
    fun taxesFromDifferentBrokersOnSameDateAreMatchedSeparately() {
        // Schwab CSCO and E-Trade CSCO can pay on the same date with different withholding rates.
        // Each (broker, symbol, date) bucket must be paired independently — the old 15% heuristic
        // used to attach the wrong tax row to the wrong dividend in this scenario.
        val dividends = listOf(
            dividendRecord(LocalDate(2025, 10, 22), 1426.80, symbol = "CSCO", broker = "Schwab"),
            dividendRecord(LocalDate(2025, 10, 22), 59.04, symbol = "CSCO", broker = "Morgan Stanley & Co.")
        )
        val taxes = listOf(
            taxRecord(LocalDate(2025, 10, 22), -428.04, symbol = "CSCO", broker = "Schwab"),
            taxRecord(LocalDate(2025, 10, 22), -8.86, symbol = "CSCO", broker = "Morgan Stanley & Co.")
        )

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }

        // One row per (broker, symbol, date) bucket.
        assertThat(usd.printableDividendList).hasSize(2)

        // The total tax should include BOTH tax records, not -8.86 twice (the old heuristic bug).
        assertThat(usd.totalTax).isCloseTo(-436.90, Offset.offset(0.01))
        assertThat(usd.totalTaxCrown).isCloseTo(-436.90 * 25.0, Offset.offset(0.1))
    }

    @Test
    fun multipleTaxRowsForSameDividendAreSummed() {
        // Schwab sometimes emits the gross withholding plus a same-day correction. Both rows share
        // (broker, symbol, date) and must be summed into a single net tax against the dividend.
        val dividends = listOf(
            dividendRecord(LocalDate(2025, 6, 27), 100.0, symbol = "JNJ", broker = "Schwab")
        )
        val taxes = listOf(
            taxRecord(LocalDate(2025, 6, 27), -30.0, symbol = "JNJ", broker = "Schwab"),
            taxRecord(LocalDate(2025, 6, 27), 15.0, symbol = "JNJ", broker = "Schwab"), // partial reversal on same day
        )

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }
        assertThat(usd.printableDividendList).hasSize(1)
        assertThat(usd.totalTax).isCloseTo(-15.0, Offset.offset(0.01))
    }

    @Test
    fun orphanedTaxWithoutMatchingDividendFailsLoudly() {
        val dividends = listOf(
            dividendRecord(LocalDate(2025, 3, 10), 100.0, symbol = "AAPL", broker = "Schwab")
        )
        val taxes = listOf(
            taxRecord(LocalDate(2025, 3, 10), -15.0, symbol = "AAPL", broker = "Schwab"),
            // Orphan: no matching dividend with this symbol.
            taxRecord(LocalDate(2025, 3, 10), -5.0, symbol = "MSFT", broker = "Schwab"),
        )

        assertThatThrownBy {
            DividentReportPreparation.generateDividendReport(
                dividends, taxes, emptyList(), year2025, fixedRate
            )
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Tax record without matching dividend")
            .hasMessageContaining("symbol=MSFT")
            .hasMessageContaining("broker=Schwab")
    }

    @Test
    fun singleDividendWithSingleTaxOnSameDate() {
        val dividends = listOf(dividendRecord(LocalDate(2025, 1, 22), 1000.0))
        val taxes = listOf(taxRecord(LocalDate(2025, 1, 22), -150.0))

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
            DividendRecord(LocalDate(2025, 6, 15), 500.0, Currency.USD, symbol = "ACME", broker = "Schwab", country = "US")
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
            taxRecord(LocalDate(2025, 1, 22), -150.0),
            taxRecord(LocalDate(2025, 4, 23), -180.0)
        )

        val report = DividentReportPreparation.generateDividendReport(
            dividends, taxes, emptyList(), year2025, fixedRate
        )

        val usd = report.sections.single { it.currency == Currency.USD }
        assertThat(usd.printableDividendList).hasSize(2)
        assertThat(usd.totalTax).isCloseTo(-330.0, Offset.offset(0.01))
    }
}
