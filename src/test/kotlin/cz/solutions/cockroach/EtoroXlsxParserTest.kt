package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.joda.time.LocalDate
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EtoroXlsxParserTest {

    private val DEFAULT_HEADERS = listOf(
        "Date of Payment",         // A
        "Instrument Name",         // B
        "Net Dividend Received",   // C
        "x", "x", "x", "x", "x",   // D..H (unused by the parser)
        "Withholding Tax Amount",  // I
    )

    @Test
    fun parsesDividendAndWithholdingTaxFromSyntheticWorkbook(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, listOf(
            EtoroRow(date = "01/02/2025", instrument = "AAPL", net = 0.85, wht = 0.15),
            EtoroRow(date = "15/06/2025", instrument = "VOD.L", net = 4.50, wht = 0.50),
        ))

        val result = EtoroXlsxParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 2, 1), 1.00, Currency.USD, symbol = "AAPL", broker = "eToro", country = "US"),
            DividendRecord(LocalDate(2025, 6, 15), 5.00, Currency.USD, symbol = "VOD.L", broker = "eToro", country = "US"),
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 2, 1), -0.15, Currency.USD, symbol = "AAPL", broker = "eToro"),
            TaxRecord(LocalDate(2025, 6, 15), -0.50, Currency.USD, symbol = "VOD.L", broker = "eToro"),
        )
    }

    @Test
    fun grossDividendIsNetPlusWithholdingTax(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, listOf(EtoroRow("10/03/2025", "MSFT", net = 0.40, wht = 0.10)))

        val result = EtoroXlsxParser.parse(file)

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.dividendRecords[0].amount).isEqualTo(0.50, within(1e-9))
        assertThat(result.taxRecords[0].amount).isEqualTo(-0.10, within(1e-9))
    }

    @Test
    fun rowsWithoutWithholdingTaxProduceNoTaxRecord(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, listOf(EtoroRow("05/07/2025", "TSLA", net = 1.00, wht = 0.0)))

        val result = EtoroXlsxParser.parse(file)

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.dividendRecords[0].amount).isEqualTo(1.00, within(1e-9))
        assertThat(result.taxRecords).isEmpty()
    }

    @Test
    fun nonPositiveGrossDividendIsSkipped(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, listOf(
            EtoroRow("01/01/2025", "ZZZ", net = 0.0, wht = 0.0),
            EtoroRow("02/01/2025", "AAPL", net = 0.85, wht = 0.15),
        ))

        val result = EtoroXlsxParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 1, 2), 1.00, Currency.USD, symbol = "AAPL", broker = "eToro", country = "US")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 1, 2), -0.15, Currency.USD, symbol = "AAPL", broker = "eToro")
        )
    }

    @Test
    fun headerMismatchFailsFast(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, rows = emptyList(), headers = listOf(
            "Wrong", "Instrument Name", "Net Dividend Received",
            "x", "x", "x", "x", "x", "Withholding Tax Amount",
        ))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            EtoroXlsxParser.parse(file)
        }
        assertThat(ex.message).contains("unexpected header in column A")
    }

    // --- synthetic XLSX helpers --------------------------------------------------

    private data class EtoroRow(val date: String, val instrument: String, val net: Double, val wht: Double)

    private fun writeEtoroLikeXlsx(
        file: File,
        rows: List<EtoroRow>,
        headers: List<String> = DEFAULT_HEADERS,
    ) {
        val pool = LinkedHashMap<String, Int>()
        fun intern(s: String): Int = pool.getOrPut(s) { pool.size }
        headers.forEach { intern(it) }
        rows.forEach { intern(it.date); intern(it.instrument) }

        val sharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${pool.size}" uniqueCount="${pool.size}">""")
            pool.keys.forEach { append("<si><t>").append(escapeXml(it)).append("</t></si>") }
            append("</sst>")
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            // eToro emits cell attributes as r="..." s="0" t="s" (i.e. style before type),
            // which previously broke the parser regex. We reproduce that exact ordering
            // here so the test locks down the regression.
            append("""<row r="1">""")
            headers.forEachIndexed { i, h ->
                append("""<c r="${col(i)}1" s="0" t="s"><v>${pool[h]}</v></c>""")
            }
            append("</row>")
            rows.forEachIndexed { idx, row ->
                val rNum = idx + 2
                append("""<row r="$rNum">""")
                append("""<c r="A$rNum" s="0" t="s"><v>${pool[row.date]}</v></c>""")
                append("""<c r="B$rNum" s="0" t="s"><v>${pool[row.instrument]}</v></c>""")
                append("""<c r="C$rNum" s="7"><v>${row.net}</v></c>""")
                append("""<c r="I$rNum" s="7"><v>${row.wht}</v></c>""")
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun col(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            sb.append(('A' + r))
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
