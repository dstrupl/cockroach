package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
            DividendRecord(LocalDate(2025, 12, 1), 10.0, Currency.USD, "DIV1", "Interactive Brokers", "US"),
            DividendRecord(LocalDate(2025, 11, 13), 20.0, Currency.USD, "DIV2", "Interactive Brokers", "US")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 12, 1), -1.5, Currency.USD, "DIV1", "Interactive Brokers"),
            TaxRecord(LocalDate(2025, 11, 13), -3.0, Currency.USD, "DIV2", "Interactive Brokers")
        )
    }

    @Test
    fun parsesRecordCountFromIbCsv() {
        val file = loadResourceAsFile("ib_dividend.csv")

        val result = DividendIBParser.parse(file)

        assertThat(result.dividendRecords).hasSize(2)
        assertThat(result.taxRecords).hasSize(2)
    }

    @Test
    fun dividendWithoutWithholdingRowGetsExplicitZeroTaxRecord() {
        val csv = """
            Summary,Data,Base Currency,USD
            Transaction History,Header,Date,Account,Description,Transaction Type,Symbol,Quantity,Price,Price Currency,Gross Amount,Commission,Net Amount
            Transaction History,Data,2025-07-05,U123,ACME,Dividend,ACME,-,-,-,10.0,-,10.0
        """.trimIndent()

        val result = DividendIBParser.parse(csv.byteInputStream())

        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 7, 5), 0.0, Currency.USD, "ACME", "Interactive Brokers")
        )
    }

    @Test
    fun changedTransactionLayoutFailsFast() {
        val csv = """
            Summary,Data,Base Currency,USD
            Transaction History,Header,Wrong Date,Account,Description,Transaction Type,Symbol,Quantity,Price,Price Currency,Gross Amount,Commission,Net Amount
        """.trimIndent()

        assertThatIllegalArgumentException()
            .isThrownBy { DividendIBParser.parse(csv.byteInputStream()) }
            .withMessageContaining("column 2")
            .withMessageContaining("expected 'Date'")
    }
}
