package cz.solutions.cockroach

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File

class DegiroAccountStatementParserTest {

    enum class Format(val extension: String, val newWorkbook: () -> Workbook) {
        XLS("xls", { HSSFWorkbook() }),
        XLSX("xlsx", { XSSFWorkbook() })
    }

    private data class Row(
        val date: String,
        val time: String,
        val valueDate: String,
        val product: String,
        val isin: String,
        val description: String,
        val currency: String,
        val amount: String
    )

    @ParameterizedTest
    @EnumSource(Format::class)
    fun parsesDividendAndTaxRecords(format: Format, @TempDir tempDir: File) {
        val file = File(tempDir, "degiro.${format.extension}")
        createWorkbook(format, file, listOf(
            Row("15-03-2024", "10:00", "15-03-2024", "APPLE INC", "US0378331005", "Dividenda", "USD", "12,34"),
            Row("15-03-2024", "10:00", "15-03-2024", "APPLE INC", "US0378331005", "Daň z dividendy", "USD", "-1,85")
        ))

        val result = DegiroAccountStatementParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2024, 3, 15), 12.34, Currency.USD, symbol = "APPLE INC", broker = "Degiro", country = "US")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2024, 3, 15), -1.85, Currency.USD, symbol = "APPLE INC", broker = "Degiro")
        )
    }

    @ParameterizedTest
    @EnumSource(Format::class)
    fun usesValueDateNotBookingDate(format: Format, @TempDir tempDir: File) {
        val file = File(tempDir, "degiro.${format.extension}")
        createWorkbook(format, file, listOf(
            Row("16-03-2024", "10:00", "15-03-2024", "APPLE INC", "US0378331005", "Dividenda", "USD", "10,00")
        ))

        val result = DegiroAccountStatementParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2024, 3, 15), 10.00, Currency.USD, symbol = "APPLE INC", broker = "Degiro", country = "US")
        )
    }

    @ParameterizedTest
    @EnumSource(Format::class)
    fun parsesEurAndCzkCurrencies(format: Format, @TempDir tempDir: File) {
        val file = File(tempDir, "degiro.${format.extension}")
        createWorkbook(format, file, listOf(
            Row("01-04-2024", "10:00", "01-04-2024", "ASML HOLDING", "NL0010273215", "Dividenda", "EUR", "5,00"),
            Row("02-04-2024", "10:00", "02-04-2024", "CEZ AS", "CZ0005112300", "Dividenda", "CZK", "1 234,56")
        ))

        val result = DegiroAccountStatementParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2024, 4, 1), 5.00, Currency.EUR, symbol = "ASML HOLDING", broker = "Degiro", country = "NL"),
            DividendRecord(LocalDate(2024, 4, 2), 1234.56, Currency.CZK, symbol = "CEZ AS", broker = "Degiro", country = "CZ")
        )
    }

    @ParameterizedTest
    @EnumSource(Format::class)
    fun ignoresAdrFeesAndUnrelatedRows(format: Format, @TempDir tempDir: File) {
        val file = File(tempDir, "degiro.${format.extension}")
        createWorkbook(format, file, listOf(
            Row("10-05-2024", "10:00", "10-05-2024", "APPLE INC", "US0378331005", "Dividenda", "USD", "20,00"),
            Row("11-05-2024", "10:00", "11-05-2024", "ADR ON ALIBABA", "US01609W1027", "ADR/GDR Pass-Through poplatek", "USD", "-0,05"),
            Row("12-05-2024", "10:00", "12-05-2024", "", "", "FX vyučtování konverze měny", "USD", "-1,00"),
            Row("13-05-2024", "10:00", "13-05-2024", "", "", "Vklad", "EUR", "100,00")
        ))

        val result = DegiroAccountStatementParser.parse(file)

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.taxRecords).isEmpty()
    }

    private fun createWorkbook(format: Format, targetFile: File, rows: List<Row>) {
        format.newWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Přehled účtu")
            val header = sheet.createRow(0)
            listOf("Datum", "Čas", "Datum (valuty)", "Produkt", "ISIN", "Popis",
                   "Kurz", "Pohyb-currency", "Pohyb-amount", "Zůstatek-currency",
                   "Zůstatek-amount", "ID objednávky")
                .forEachIndexed { i, name -> header.createCell(i).setCellValue(name) }
            rows.forEachIndexed { index, r ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(r.date)
                row.createCell(1).setCellValue(r.time)
                row.createCell(2).setCellValue(r.valueDate)
                row.createCell(3).setCellValue(r.product)
                row.createCell(4).setCellValue(r.isin)
                row.createCell(5).setCellValue(r.description)
                row.createCell(7).setCellValue(r.currency)
                row.createCell(8).setCellValue(r.amount)
            }
            targetFile.outputStream().use { workbook.write(it) }
        }
    }
}
