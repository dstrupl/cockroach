package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class ETradeBenefitHistoryParserTest {

    companion object {
        private const val FIXTURE = "/cz/solutions/cockroach/BenefitHistory.xlsx"
        private const val EPS = 0.001
    }

    private fun parseFixture(): ETradeBenefitHistoryResult =
        checkNotNull(this::class.java.getResourceAsStream(FIXTURE)) { "missing test fixture $FIXTURE" }
            .use { ETradeBenefitHistoryParser.parse(it) }

    @Test
    fun parsesEsppPurchasesFromBenefitHistory() {
        val result = parseFixture()

        assertThat(result.esppRecords).hasSize(3)

        val purchase2025Q1 = result.esppRecords.single { it.purchaseDate == LocalDate(2025, 3, 15) }
        assertThat(purchase2025Q1.quantity).isEqualTo(100.0, within(EPS))
        assertThat(purchase2025Q1.purchasePrice).isEqualTo(10.00, within(EPS))
        assertThat(purchase2025Q1.subscriptionFmv).isEqualTo(12.00, within(EPS))
        assertThat(purchase2025Q1.purchaseFmv).isEqualTo(15.00, within(EPS))
        assertThat(purchase2025Q1.symbol).isEqualTo("ACME")
        assertThat(purchase2025Q1.broker).isEqualTo("Morgan Stanley & Co.")

        val purchase2025Q3 = result.esppRecords.single { it.purchaseDate == LocalDate(2025, 9, 15) }
        assertThat(purchase2025Q3.purchaseFmv).isEqualTo(20.00, within(EPS))

        val purchase2026Q1 = result.esppRecords.single { it.purchaseDate == LocalDate(2026, 3, 15) }
        assertThat(purchase2026Q1.purchaseFmv).isEqualTo(18.00, within(EPS))
    }

    @Test
    fun parsesRsuVestsFromBenefitHistory() {
        val result = parseFixture()

        // Future grants (G-1003 with no vested shares) must be excluded.
        assertThat(result.rsuRecords.map { it.grantId }).containsOnly("G-1001", "G-1002")

        val grant1001 = result.rsuRecords.filter { it.grantId == "G-1001" }
        assertThat(grant1001).hasSize(4)
        assertThat(grant1001).allSatisfy { record ->
            assertThat(record.symbol).isEqualTo("ACME")
            assertThat(record.broker).isEqualTo("Morgan Stanley & Co.")
        }
        assertThat(grant1001.first { it.vestDate == LocalDate(2025, 6, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(50) },
                       { assertThat(it.vestFmv).isEqualTo(15.00, within(EPS)) })
        assertThat(grant1001.first { it.vestDate == LocalDate(2025, 9, 20) }.vestFmv)
            .isEqualTo(20.00, within(EPS))

        val grant1002 = result.rsuRecords.filter { it.grantId == "G-1002" }
        assertThat(grant1002).hasSize(4)
        assertThat(grant1002.first { it.vestDate == LocalDate(2025, 6, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(200) },
                       { assertThat(it.vestFmv).isEqualTo(15.00, within(EPS)) })
        assertThat(grant1002.first { it.vestDate == LocalDate(2026, 3, 20) })
            .satisfies({ assertThat(it.quantity).isEqualTo(200) },
                       { assertThat(it.vestFmv).isEqualTo(25.00, within(EPS)) })
    }
}
