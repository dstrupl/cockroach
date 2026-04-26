package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EtoroXlsxParserTest {

    @Test
    fun parsesDividendAndWithholdingTaxFromSyntheticWorkbook(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        writeEtoroLikeXlsx(file, listOf(
            EtoroRow(date = "01/02/2025", instrument = "AAPL", net = 0.85, wht = 0.15),
            EtoroRow(date = "15/06/2025", instrument = "VOD.L", net = 4.50, wht = 0.50),
        ))

        val result = EtoroXlsxParser.parse(file)

        assertThat(result.dividendRecords).containsExactly(
            DividendRecord(LocalDate(2025, 2, 1), 1.00, Currency.USD, symbol = "AAPL", broker = "eToro"),
            DividendRecord(LocalDate(2025, 6, 15), 5.00, Currency.USD, symbol = "VOD.L", broker = "eToro"),
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 2, 1), -0.15, Currency.USD),
            TaxRecord(LocalDate(2025, 6, 15), -0.50, Currency.USD),
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
            DividendRecord(LocalDate(2025, 1, 2), 1.00, Currency.USD, symbol = "AAPL", broker = "eToro")
        )
        assertThat(result.taxRecords).containsExactly(
            TaxRecord(LocalDate(2025, 1, 2), -0.15, Currency.USD)
        )
    }

    @Test
    fun headerMismatchFailsFast(@TempDir tempDir: File) {
        val file = File(tempDir, "etoro.xlsx")
        // Write a workbook whose column A header is wrong; parser must reject it.
        writeEtoroLikeXlsx(file, rows = emptyList(), headers = listOf(
            "Wrong", "Instrument Name", "Net Dividend Received",
            "x", "x", "x", "x", "x", "Withholding Tax Amount"
        ))

        val ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            EtoroXlsxParser.parse(file)
        }
        assertThat(ex.message).contains("unexpected header in column A")
    }

    // ---- synthetic XLSX builder ----------------------------------------------------

    private data class EtoroRow(val date: String, val instrument: String, val net: Double, val wht: Double)

    private val DEFAULT_HEADERS = listOf(
        "Date of Payment",
        "Instrument Name",
        "Net Dividend Received",
        "x", "x", "x", "x", "x",
        "Withholding Tax Amount",
    )

    private fun writeEtoroLikeXlsx(file: File, rows: List<EtoroRow>, headers: List<String> = DEFAULT_HEADERS) {
        // Build sharedStrings: headers first, then per-row instruments (deduplicated).
        val instruments = rows.map { it.instrument }.distinct()
        val strings = headers + instruments
        val instrumentIndex = instruments.withIndex().associate { (i, v) -> v to headers.size + i }

        val sharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
            strings.forEach { append("<si><t>").append(escapeXml(it)).append("</t></si>") }
            append("</sst>")
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            // header row uses shared-string indexes 0..headers.size-1; eToro emits attributes as
            // r="..." s="0" t="s" – we reproduce that exact ordering to lock down the regex fix.
            append("""<row r="1">""")
            headers.forEachIndexed { i, _ ->
                val ref = "${('A' + i)}1"
                append("""<c r="$ref" s="0" t="s"><v>$i</v></c>""")
            }
            append("</row>")
            rows.forEachIndexed { idx, row ->
                val rNum = idx + 2
                append("""<row r="$rNum">""")
                append("""<c r="A$rNum" s="0" t="s"><v>${headers.size + headers.size /* placeholder */}</v></c>""".replace("<v>${headers.size + headers.size}</v>", "<v>${stringIndexFor(row.date, strings)}</v>"))
                // simpler: rewrite cleanly below
                setLength(length - "</row>".length - "<c r=\"A$rNum\" s=\"0\" t=\"s\"><v>${stringIndexFor(row.date, strings)}</v></c>".length - "<row r=\"$rNum\">".length)
                append("""<row r="$rNum">""")
                // Date is also a shared string in eToro statements
                val dateIdx = ensureString(row.date, strings, sharedStringsAddenda)
                append("""<c r="A$rNum" s="0" t="s"><v>$dateIdx</v></c>""")
                append("""<c r="B$rNum" s="0" t="s"><v>${instrumentIndex[row.instrument]}</v></c>""")
                append("""<c r="C$rNum" s="7"><v>${row.net}</v></c>""")
                append("""<c r="I$rNum" s="7"><v>${row.wht}</v></c>""")
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }

        // Recompose the final shared strings including any dates appended on the fly.
        val finalStrings = strings + sharedStringsAddenda
        val finalSharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${finalStrings.size}" uniqueCount="${finalStrings.size}">""")
            finalStrings.forEach { append("<si><t>").append(escapeXml(it)).append("</t></si>") }
            append("</sst>")
        }

        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(finalSharedStringsXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        sharedStringsAddenda.clear()
    }

    private val sharedStringsAddenda = mutableListOf<String>()

    private fun stringIndexFor(value: String, strings: List<String>): Int {
        val i = strings.indexOf(value)
        return if (i >= 0) i else strings.size + sharedStringsAddenda.indexOf(value).also { if (it < 0) sharedStringsAddenda.add(value) }
    }

    private fun ensureString(value: String, strings: List<String>, addenda: MutableList<String>): Int {
        val i = strings.indexOf(value)
        if (i >= 0) return i
        val j = addenda.indexOf(value)
        if (j >= 0) return strings.size + j
        addenda.add(value)
        return strings.size + addenda.size - 1
    }

    private fun escapeXml(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
