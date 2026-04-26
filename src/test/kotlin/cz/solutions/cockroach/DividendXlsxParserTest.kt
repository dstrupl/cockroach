package cz.solutions.cockroach

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DividendXlsxParserTest {

    private fun loadResourceAsFile(name: String): File {
        return File({}::class.java.getResource(name)!!.toURI())
    }

    @Test
    fun parsesDividendAndTaxRecords() {
        val file = loadResourceAsFile("cash_transactions.xlsx")

        val result = DividendXlsxParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 10, 22), 58.22, symbol = "CISCO SYS INC", broker = "Morgan Stanley & Co.", country = "US")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 10, 22), -8.73)
        )
    }

    @Test
    fun parsesRecordCount() {
        val file = loadResourceAsFile("cash_transactions.xlsx")

        val result = DividendXlsxParser.parse(file)

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.taxRecords).hasSize(1)
    }

    @Test
    fun ignoresTreasuryLiquidityFundAndWireOutRows(@TempDir tempDir: File) {
        val file = File(tempDir, "transactions.xlsx")
        createXlsx(file, listOf(
            Triple("10/22/2025", "CISCO SYS INC REC 10/22/25 PAY 10/22/25", 58.22),
            Triple("10/22/2025", "CISCO SYS INC NON RESIDENT WITHHOLDING", -8.73),
            Triple("10/23/2025", "TREASURY LIQUIDITY FUND DIV PAYMENT", 0.12),
            Triple("10/24/2025", "WIRE OUT", -500.00)
        ))

        val result = DividendXlsxParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 10, 22), 58.22, symbol = "CISCO SYS INC", broker = "Morgan Stanley & Co.", country = "US")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 10, 22), -8.73)
        )
    }

    private fun createXlsx(targetFile: File, rows: List<Triple<String, String, Double>>) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("Date")
            header.createCell(1).setCellValue("Description")
            header.createCell(2).setCellValue("Value $")
            rows.forEachIndexed { index, (date, description, value) ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(date)
                row.createCell(1).setCellValue(description)
                row.createCell(2).setCellValue(value)
            }
            targetFile.outputStream().use { workbook.write(it) }
        }
    }
}
