package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import java.io.File

class ETradeGainLossXlsParserTest {

    private fun loadResourceAsFile(name: String): File {
        return File({}::class.java.getResource(name)!!.toURI())
    }

    @Test
    fun `parses sale records from xlsx`() {
        val file = loadResourceAsFile("gain_loss_test.xlsx")

        val result = ETradeGainLossXlsParser.parse(file)

        assertThat(result).hasSize(2)

        assertThat(result[0]).isEqualTo(
            SaleRecord(
                LocalDate(2026, 2, 9), "RS", 25.0, 86.7548, 71.79, 71.79,
                LocalDate(2025, 8, 10), "1538646"
            )
        )
        assertThat(result[1]).isEqualTo(
            SaleRecord(
                LocalDate(2026, 2, 9), "RS", 47.0, 86.754894, 71.79, 71.79,
                LocalDate(2025, 8, 10), "1642365"
            )
        )
    }

}
