package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.joda.time.LocalDate
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class ETradeBenefitHistoryParserTest {

    companion object {
        private val ACTUAL_FILE = File("input/BenefitHistory.xlsx")
        private const val EPS = 0.001
    }

    @Test
    fun parsesEsppPurchasesFromBenefitHistory() {
        assumeTrue(ACTUAL_FILE.exists(), "input/BenefitHistory.xlsx not available, skipping")

        val result = ETradeBenefitHistoryParser.parse(ACTUAL_FILE)

        assertThat(result.esppRecords).hasSize(3)

        val purchase2025Q1 = result.esppRecords.single { it.purchaseDate == LocalDate(2025, 3, 15) }
        assertThat(purchase2025Q1.quantity).isEqualTo(177.0, within(EPS))
        assertThat(purchase2025Q1.purchasePrice).isEqualTo(42.143, within(EPS))
        assertThat(purchase2025Q1.subscriptionFmv).isEqualTo(49.58, within(EPS))
        assertThat(purchase2025Q1.purchaseFmv).isEqualTo(50.92, within(EPS))
        assertThat(purchase2025Q1.symbol).isNotBlank()
        assertThat(purchase2025Q1.broker).isEqualTo("Morgan Stanley & Co.")

        val purchase2025Q3 = result.esppRecords.single { it.purchaseDate == LocalDate(2025, 9, 15) }
        assertThat(purchase2025Q3.purchaseFmv).isEqualTo(86.76, within(EPS))

        val purchase2026Q1 = result.esppRecords.single { it.purchaseDate == LocalDate(2026, 3, 15) }
        assertThat(purchase2026Q1.purchaseFmv).isEqualTo(61.50, within(EPS))
    }

    @Test
    fun parsesRsuVestsFromBenefitHistory() {
        assumeTrue(ACTUAL_FILE.exists(), "input/BenefitHistory.xlsx not available, skipping")

        val result = ETradeBenefitHistoryParser.parse(ACTUAL_FILE)

        // Future grants (e.g. 3-89431 with no vested shares) must be excluded.
        assertThat(result.rsuRecords.map { it.grantId }).containsOnly("3-82769", "3-74067")

        val grant82769 = result.rsuRecords.filter { it.grantId == "3-82769" }
        assertThat(grant82769).hasSize(4)
        assertThat(grant82769).allSatisfy { record ->
            assertThat(record.symbol).isNotBlank()
            assertThat(record.broker).isEqualTo("Morgan Stanley & Co.")
        }
        assertThat(grant82769.first { it.vestDate == LocalDate(2025, 6, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(66) },
                       { assertThat(it.vestFmv).isEqualTo(52.870, within(EPS)) })
        assertThat(grant82769.first { it.vestDate == LocalDate(2025, 9, 20) }.vestFmv)
            .isEqualTo(87.870, within(EPS))

        val grant74067 = result.rsuRecords.filter { it.grantId == "3-74067" }
        assertThat(grant74067).hasSize(4)
        assertThat(grant74067.first { it.vestDate == LocalDate(2025, 6, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(829) },
                       { assertThat(it.vestFmv).isEqualTo(52.870, within(EPS)) })
        assertThat(grant74067.first { it.vestDate == LocalDate(2026, 3, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(208) },
                       { assertThat(it.vestFmv).isEqualTo(65.450, within(EPS)) })
    }
}
