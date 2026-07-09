package cz.solutions.cockroach

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.InputStream
import java.util.Locale

data class ETradeBenefitHistoryResult(
    val rsuRecords: List<RsuRecord>,
    val esppRecords: List<EsppRecord>
)

object ETradeBenefitHistoryParser {

    private val PURCHASE_DATE_FORMATTER = DateTimeFormat.forPattern("dd-MMM-yyyy").withLocale(Locale.ENGLISH)
    private val VEST_DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy")

    private const val BROKER_NAME = "Morgan Stanley & Co."

    private const val COL_RECORD_TYPE = "Record Type"
    private const val COL_SYMBOL = "Symbol"

    private const val COL_PURCHASE_DATE = "Purchase Date"
    private const val COL_PURCHASE_PRICE = "Purchase Price"
    private const val COL_PURCHASED_QTY = "Purchased Qty."
    private const val COL_GRANT_DATE_FMV = "Grant Date FMV"
    private const val COL_PURCHASE_DATE_FMV = "Purchase Date FMV"

    private const val COL_GRANT_NUMBER = "Grant Number"
    private const val COL_VEST_PERIOD = "Vest Period"
    private const val COL_VEST_DATE = "Vest Date"
    private const val COL_VESTED_QTY = "Vested Qty."
    private const val COL_TAXABLE_GAIN = "Taxable Gain"

    private const val REC_PURCHASE = "Purchase"
    private const val REC_GRANT = "Grant"
    private const val REC_VEST_SCHEDULE = "Vest Schedule"
    private const val REC_TAX_WITHHOLDING = "Tax Withholding"

    fun parse(file: File): ETradeBenefitHistoryResult = file.inputStream().use { parse(it) }

    fun parse(inputStream: InputStream): ETradeBenefitHistoryResult {
        XSSFWorkbook(inputStream).use { workbook ->
            var espp = emptyList<EsppRecord>()
            var rsu = emptyList<RsuRecord>()
            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                val header = sheet.getRow(sheet.firstRowNum) ?: continue
                val cols = buildColumnIndex(header)
                when {
                    cols.containsKey(COL_PURCHASED_QTY) -> espp = parseEspp(sheet, cols)
                    cols.containsKey(COL_TAXABLE_GAIN) -> rsu = parseRsu(sheet, cols)
                }
            }
            return ETradeBenefitHistoryResult(rsuRecords = rsu, esppRecords = espp)
        }
    }

    private fun parseEspp(sheet: Sheet, cols: Map<String, Int>): List<EsppRecord> {
        val out = mutableListOf<EsppRecord>()
        for (i in (sheet.firstRowNum + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            if (str(row, cols, COL_RECORD_TYPE) != REC_PURCHASE) continue
            val date = LocalDate.parse(requireStr(row, cols, COL_PURCHASE_DATE).uppercase(), PURCHASE_DATE_FORMATTER)
            out += EsppRecord(
                date = date,
                quantity = num(row, cols, COL_PURCHASED_QTY),
                purchasePrice = num(row, cols, COL_PURCHASE_PRICE),
                subscriptionFmv = num(row, cols, COL_GRANT_DATE_FMV),
                purchaseFmv = num(row, cols, COL_PURCHASE_DATE_FMV),
                purchaseDate = date,
                symbol = str(row, cols, COL_SYMBOL)!!,
                broker = BROKER_NAME
            )
        }
        return out
    }

    private fun parseRsu(sheet: Sheet, cols: Map<String, Int>): List<RsuRecord> {
        // Pair Vest Schedule rows (where Vested Qty > 0) with their matching Tax Withholding row
        // identified by (Grant Number, Vest Period) appearing immediately below.
        data class Pending(val grantId: String, val period: String, val vestDate: LocalDate, val vestedQty: Int)

        val symbolByGrant = mutableMapOf<String, String>()
        val out = mutableListOf<RsuRecord>()
        var pending: Pending? = null
        for (i in (sheet.firstRowNum + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            when (str(row, cols, COL_RECORD_TYPE)) {
                REC_GRANT -> {
                    pending = null
                    val grantId = str(row, cols, COL_GRANT_NUMBER) ?: continue
                    val symbol = str(row, cols, COL_SYMBOL) ?: continue
                    symbolByGrant[grantId] = symbol
                }
                REC_VEST_SCHEDULE -> {
                    pending = null
                    val vested = optNum(row, cols, COL_VESTED_QTY)?.toInt() ?: 0
                    if (vested <= 0) continue
                    pending = Pending(
                        grantId = requireStr(row, cols, COL_GRANT_NUMBER),
                        period = requireStr(row, cols, COL_VEST_PERIOD),
                        vestDate = LocalDate.parse(requireStr(row, cols, COL_VEST_DATE), VEST_DATE_FORMATTER),
                        vestedQty = vested
                    )
                }
                REC_TAX_WITHHOLDING -> {
                    val p = pending ?: continue
                    if (str(row, cols, COL_GRANT_NUMBER) != p.grantId) { pending = null; continue }
                    if (str(row, cols, COL_VEST_PERIOD) != p.period) { pending = null; continue }
                    val taxableGain = num(row, cols, COL_TAXABLE_GAIN)
                    out += RsuRecord(
                        date = p.vestDate,
                        quantity = p.vestedQty,
                        vestFmv = taxableGain / p.vestedQty,
                        vestDate = p.vestDate,
                        grantId = p.grantId,
                        symbol = symbolByGrant[p.grantId]!!,
                        broker = BROKER_NAME
                    )
                    pending = null
                }
                else -> pending = null
            }
        }
        return out
    }

    private fun buildColumnIndex(headerRow: Row): Map<String, Int> {
        // The Restricted Stock sheet repeats some header names across row types
        // (e.g. "Vested Qty." appears for the Grant total and again for each Vest Schedule).
        // The per-row-type columns sit further to the right, so the later occurrence wins.
        val index = mutableMapOf<String, Int>()
        for (i in 0..headerRow.lastCellNum) {
            val cell = headerRow.getCell(i) ?: continue
            if (cell.cellType == CellType.STRING) index[cell.stringCellValue.trim()] = i
        }
        return index
    }

    private fun str(row: Row, cols: Map<String, Int>, name: String): String? {
        val idx = cols[name] ?: return null
        val cell = row.getCell(idx) ?: return null
        return cellAsString(cell).takeIf { it.isNotBlank() }
    }

    private fun requireStr(row: Row, cols: Map<String, Int>, name: String): String =
        str(row, cols, name) ?: throw IllegalArgumentException("Missing '$name' in row ${row.rowNum + 1}")

    private fun num(row: Row, cols: Map<String, Int>, name: String): Double =
        optNum(row, cols, name) ?: throw IllegalArgumentException("Missing '$name' in row ${row.rowNum + 1}")

    private fun optNum(row: Row, cols: Map<String, Int>, name: String): Double? {
        val raw = str(row, cols, name) ?: return null
        return raw.replace("$", "").replace("\u00a0", "").replace(",", "").trim().toDouble()
    }

    private fun cellAsString(cell: Cell): String = when (cell.cellType) {
        CellType.STRING -> cell.stringCellValue.trim()
        CellType.NUMERIC -> {
            val d = cell.numericCellValue
            if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
        }
        CellType.BOOLEAN -> cell.booleanCellValue.toString()
        CellType.FORMULA -> cell.toString().trim()
        else -> ""
    }
}
