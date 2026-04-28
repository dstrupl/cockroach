package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import java.io.File

class DividendIBParserTest {

    private fun loadResourceAsFile(name: String): File {
        return File({}::class.java.getResource(name)!!.toURI())
    }

    @Test
    fun parsesDividendAndTaxRecordsFromIbCsv() {
        val file = loadResourceAsFile("ib_dividend.csv")

        val result = DividendIBParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 12, 1), 10.0),
            DividendRecord(LocalDate(2025, 11, 13), 20.0)
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 12, 1), -1.5),
            TaxRecord(LocalDate(2025, 11, 13), -3.0)
        )
    }

    @Test
    fun parsesRecordCountFromIbCsv() {
        val file = loadResourceAsFile("ib_dividend.csv")

        val result = DividendIBParser.parse(file)

        assertThat(result.dividendRecords).hasSize(2)
        assertThat(result.taxRecords).hasSize(2)
    }
}