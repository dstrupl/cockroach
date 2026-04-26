package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class ETradeGainLossParserTest {

    private fun loadResource(file: String) = {}::class.java.getResource(file)?.readText()

    @Test
    fun `parses E-Trade gain and loss CSV`() {
        val actual = loadResource("etrade_gain_loss.csv")?.let {
            ETradeGainLossParser.parse(it)
        }

        assertThat(actual).isNotNull
        assertThat(actual).hasSize(3)

        assertThat(actual?.get(0)).isEqualTo(
            SaleRecord(
                LocalDate(2025, 3, 16),
                "RS",
                10.0,
                52.00,
                50.00,
                50.00,
                LocalDate(2025, 3, 15),
                "9990001",
                symbol = "ACME",
                broker = "Morgan Stanley & Co."
            )
        )

        assertThat(actual?.get(1)).isEqualTo(
            SaleRecord(
                LocalDate(2025, 3, 16),
                "RS",
                25.0,
                65.00,
                60.00,
                60.00,
                LocalDate(2025, 3, 15),
                "9990002",
                symbol = "ACME",
                broker = "Morgan Stanley & Co."
            )
        )

        assertThat(actual?.get(2)).isEqualTo(
            SaleRecord(
                LocalDate(2025, 6, 12),
                "RS",
                65.0,
                75.00,
                70.00,
                70.00,
                LocalDate(2025, 6, 10),
                "9990001",
                symbol = "ACME",
                broker = "Morgan Stanley & Co."
            )
        )

        
    }

    @Test
    fun `merging ParsedExports concatenates all record lists`() {
        val schwab = ParsedExport(
            rsuRecords = listOf(RsuRecord(LocalDate(2023, 12, 10), 2, 48.38, LocalDate(2023, 12, 10), "1461994")),
            esppRecords = emptyList(),
            dividendRecords = listOf(DividendRecord(LocalDate(2023, 10, 25), 84.38)),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = listOf(SaleRecord(LocalDate(2023, 9, 27), "RS", 30.0, 47.62, 43.91, 43.91, LocalDate(2022, 11, 10), "1538646")),
            journalRecords = emptyList()
        )
        val eTrade = ParsedExport(
            rsuRecords = listOf(RsuRecord(LocalDate(2025, 3, 15), 10, 50.00, LocalDate(2025, 3, 15), "9990001")),
            esppRecords = emptyList(),
            dividendRecords = emptyList(),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = listOf(SaleRecord(LocalDate(2025, 3, 16), "RS", 10.0, 52.00, 50.00, 50.00, LocalDate(2025, 3, 15), "9990001")),
            journalRecords = emptyList()
        )

        val merged = schwab + eTrade

        assertThat(merged.rsuRecords).hasSize(2)
        assertThat(merged.rsuRecords[0].grantId).isEqualTo("1461994")
        assertThat(merged.rsuRecords[1].grantId).isEqualTo("9990001")
        assertThat(merged.saleRecords).hasSize(2)
        assertThat(merged.saleRecords[0].grantId).isEqualTo("1538646")
        assertThat(merged.saleRecords[1].grantId).isEqualTo("9990001")
        assertThat(merged.dividendRecords).hasSize(1)
        assertThat(merged.esppRecords).isEmpty()
    }

    @Test
    fun `empty ParsedExport has all empty lists`() {
        val empty = ParsedExport.empty()

        assertThat(empty.rsuRecords).isEmpty()
        assertThat(empty.esppRecords).isEmpty()
        assertThat(empty.dividendRecords).isEmpty()
        assertThat(empty.taxRecords).isEmpty()
        assertThat(empty.taxReversalRecords).isEmpty()
        assertThat(empty.saleRecords).isEmpty()
        assertThat(empty.journalRecords).isEmpty()
    }
}
