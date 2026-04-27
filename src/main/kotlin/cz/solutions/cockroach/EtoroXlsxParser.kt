package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.util.logging.Logger
import java.util.zip.ZipFile

data class EtoroParseResult(
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>
)

/**
 * Parses an eToro account-statement XLSX. The "Dividends" sheet (sheet4) lists
 * one row per paid dividend with the net amount already received plus the
 * withholding tax that was deducted at source. Each row is converted to a
 * gross [DividendRecord] (= net + WHT) and a negative [TaxRecord] (= -WHT).
 *
 * The parser bypasses Apache POI because eToro's workbook declares the main
 * spreadsheetml namespace under the "x:" prefix, which POI's XSSF reader
 * rejects. Instead we read the underlying ZIP entries (sharedStrings.xml and
 * worksheets/sheet4.xml) directly and extract values by regex.
 */
object EtoroXlsxParser {

    private val LOGGER = Logger.getLogger(EtoroXlsxParser::class.java.name)
    private const val BROKER_NAME = "eToro"
    private const val DIVIDENDS_SHEET = "xl/worksheets/sheet4.xml"
    private const val SHARED_STRINGS = "xl/sharedStrings.xml"
    private val DATE_FORMATTER = DateTimeFormat.forPattern("dd/MM/yyyy")

    // Header row uses the text labels below; validated so we fail fast if eToro
    // changes the column layout.
    private const val EXPECTED_DATE_HEADER = "Date of Payment"
    private const val EXPECTED_NAME_HEADER = "Instrument Name"
    private const val EXPECTED_NET_HEADER = "Net Dividend Received"
    private const val EXPECTED_WHT_HEADER = "Withholding Tax Amount"

    private val SHARED_STRING_PATTERN = Regex("""<t[^>]*>([^<]*)</t>""")
    private val ROW_PATTERN = Regex("""<row[^>]*r="(\d+)"[^>]*>(.*?)</row>""")
    private val CELL_PATTERN = Regex("""<c\s+([^>]*?)\s*/?>(?:<v[^>]*>([^<]*)</v>)?""")
    private val ATTR_R_PATTERN = Regex("""\br="([A-Z]+)\d+"""")
    private val ATTR_T_PATTERN = Regex("""\bt="([a-z]+)"""")

    // Tickers carrying a non-US exchange suffix such as ".L" (LSE), ".DE" (Xetra),
    // ".PA" (Paris) — used only for a per-row warning since we hard-code country = "US".
    private val NON_US_TICKER_SUFFIX = Regex("""\.([A-Z]{1,3})\b""")

    fun parse(file: File): EtoroParseResult {
        ZipFile(file).use { zip ->
            val strings = readSharedStrings(zip)
            val sheetEntry = zip.getEntry(DIVIDENDS_SHEET)
                ?: error("eToro XLSX is missing the dividends sheet ($DIVIDENDS_SHEET) in ${file.name}")
            val sheetXml = zip.getInputStream(sheetEntry).bufferedReader(Charsets.UTF_8).readText()
            return parseSheet(sheetXml, strings, file.name)
        }
    }

    private fun readSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry(SHARED_STRINGS) ?: return emptyList()
        val xml = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
        return SHARED_STRING_PATTERN.findAll(xml).map { decodeXmlEntities(it.groupValues[1]) }.toList()
    }

    private fun parseSheet(xml: String, strings: List<String>, fileName: String): EtoroParseResult {
        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()

        val rows = ROW_PATTERN.findAll(xml).toList()
        require(rows.isNotEmpty()) { "eToro dividends sheet is empty in $fileName" }

        val header = readRow(rows.first().groupValues[2], strings)
        validateHeader(header, fileName)

        for (rowMatch in rows.drop(1)) {
            val rowNum = rowMatch.groupValues[1].toInt()
            val cells = readRow(rowMatch.groupValues[2], strings)
            val dateStr = cells["A"] ?: continue
            val instrument = cells["B"].orEmpty().ifBlank { cells["L"].orEmpty() }
            val netStr = cells["C"] ?: continue
            val whtStr = cells["I"].orEmpty()

            val date = LocalDate.parse(dateStr, DATE_FORMATTER)
            val net = netStr.toDouble()
            val wht = whtStr.toDoubleOrNull() ?: 0.0
            val gross = net + wht
            if (gross <= 0.0) {
                LOGGER.warning("eToro: skipping non-positive dividend row $rowNum in $fileName (net=$net wht=$wht)")
                continue
            }
            // The eToro dividends sheet does not expose ISIN; default to "US" since virtually all
            // eToro dividend payers are NYSE/Nasdaq listings. Non-US tickers (e.g. VOD.L) would be
            // misclassified as US-source — warn so the user can correct the report manually.
            val suffix = NON_US_TICKER_SUFFIX.find(instrument)?.groupValues?.get(1)
            if (suffix != null) {
                LOGGER.warning(
                    "eToro: instrument '$instrument' on row $rowNum looks non-US (suffix .$suffix); " +
                            "treating as country=US — verify Příloha č. 3 vs § 16a routing manually"
                )
            }
            dividends.add(DividendRecord(date, gross, Currency.USD, symbol = instrument, broker = BROKER_NAME, country = "US"))
            if (wht > 0.0) {
                taxes.add(TaxRecord(date, -wht, Currency.USD))
            }
        }
        LOGGER.info("eToro: parsed ${dividends.size} dividend(s) and ${taxes.size} withholding tax record(s) from $fileName")
        return EtoroParseResult(dividends, taxes)
    }

    private fun readRow(rowBody: String, strings: List<String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in CELL_PATTERN.findAll(rowBody)) {
            val attrs = m.groupValues[1]
            val raw = m.groupValues[2]
            if (raw.isEmpty()) continue
            val ref = ATTR_R_PATTERN.find(attrs)?.groupValues?.get(1) ?: continue
            val type = ATTR_T_PATTERN.find(attrs)?.groupValues?.get(1).orEmpty()
            val value = if (type == "s") strings.getOrNull(raw.toInt()).orEmpty() else raw
            out[ref] = value
        }
        return out
    }

    private fun validateHeader(header: Map<String, String>, fileName: String) {
        fun check(col: String, expected: String) {
            val actual = header[col].orEmpty()
            require(actual.startsWith(expected)) {
                "eToro $fileName: unexpected header in column $col, got '$actual', expected to start with '$expected'"
            }
        }
        check("A", EXPECTED_DATE_HEADER)
        check("B", EXPECTED_NAME_HEADER)
        check("C", EXPECTED_NET_HEADER)
        check("I", EXPECTED_WHT_HEADER)
    }

    private fun decodeXmlEntities(s: String): String = s
        .replace("&#10;", "\n")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}
