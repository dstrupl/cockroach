package cz.solutions.cockroach

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.InputStream

object ETradeGainLossXlsParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy")

    private const val COL_RECORD_TYPE = "Record Type"
    private const val COL_SYMBOL = "Symbol"
    private const val COL_PLAN_TYPE = "Plan Type"
    private const val COL_QUANTITY = "Quantity"
    private const val COL_ADJUSTED_COST_BASIS_PER_SHARE = "Adjusted Cost Basis Per Share"
    private const val COL_DATE_SOLD = "Date Sold"
    private const val COL_PROCEEDS_PER_SHARE = "Proceeds Per Share"
    private const val COL_GRANT_NUMBER = "Grant Number"
    private const val COL_VEST_DATE = "Vest Date"

    fun parse(file: File): List<SaleRecord> {
        return file.inputStream().use { parse(it) }
    }

    fun parse(inputStream: InputStream): List<SaleRecord> {
        val saleRecords = mutableListOf<SaleRecord>()

        XSSFWorkbook(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val headerRow = sheet.getRow(0)
                ?: throw IllegalArgumentException("Missing header row")

            val columnIndex = buildColumnIndex(headerRow)

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                val recordType = getStringValue(row, columnIndex, COL_RECORD_TYPE) ?: continue
                if (recordType != "Sell") continue

                val vestDateStr = getStringValue(row, columnIndex, COL_VEST_DATE) ?: continue
                if (vestDateStr == "--") continue

                saleRecords.add(parseSaleRecord(row, columnIndex))
            }
        }

        return saleRecords
    }

    private fun buildColumnIndex(headerRow: Row): Map<String, Int> {
        val index = mutableMapOf<String, Int>()
        for (i in 0..headerRow.lastCellNum) {
            val cell = headerRow.getCell(i)
            if (cell != null && cell.cellType == CellType.STRING) {
                index[cell.stringCellValue.trim()] = i
            }
        }
        return index
    }

    private fun parseSaleRecord(row: Row, columnIndex: Map<String, Int>): SaleRecord {
        val symbol = getStringValue(row, columnIndex, COL_SYMBOL).orEmpty()
        val dateSold = parseDate(requireString(row, columnIndex, COL_DATE_SOLD))
        val quantity = getNumericValue(row, columnIndex, COL_QUANTITY)
        val vestDate = parseDate(requireString(row, columnIndex, COL_VEST_DATE))
        val adjustedCostBasisPerShare = getNumericValue(row, columnIndex, COL_ADJUSTED_COST_BASIS_PER_SHARE)
        val proceedsPerShare = getNumericValue(row, columnIndex, COL_PROCEEDS_PER_SHARE)
        val planType = requireString(row, columnIndex, COL_PLAN_TYPE)
        val grantNumber = requireString(row, columnIndex, COL_GRANT_NUMBER)

        return SaleRecord(
            date = dateSold,
            type = planType,
            quantity = quantity,
            salePrice = proceedsPerShare,
            purchasePrice = adjustedCostBasisPerShare,
            purchaseFmv = adjustedCostBasisPerShare,
            purchaseDate = vestDate,
            grantId = grantNumber,
            symbol = symbol,
            broker = "Morgan Stanley & Co."
        )
    }

    private fun parseDate(value: String): LocalDate {
        return LocalDate.parse(value.trim(), DATE_FORMATTER)
    }

    private fun requireColumn(columnIndex: Map<String, Int>, columnName: String): Int {
        return columnIndex[columnName]
            ?: throw IllegalArgumentException("Column '$columnName' not found in header. Available: ${columnIndex.keys}")
    }

    private fun getStringValue(row: Row, columnIndex: Map<String, Int>, columnName: String): String? {
        val colIdx = requireColumn(columnIndex, columnName)
        return row.getCell(colIdx)?.stringCellValue?.trim()
    }

    private fun requireString(row: Row, columnIndex: Map<String, Int>, columnName: String): String {
        return getStringValue(row, columnIndex, columnName)
            ?: throw IllegalArgumentException("Missing value for column '$columnName' in row ${row.rowNum}")
    }

    private fun getNumericValue(row: Row, columnIndex: Map<String, Int>, columnName: String): Double {
        val colIdx = requireColumn(columnIndex, columnName)
        val cell = row.getCell(colIdx)
            ?: throw IllegalArgumentException("Missing cell for column '$columnName' in row ${row.rowNum}")
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> cell.stringCellValue
                .replace("$", "")
                .replace("\u00a0", "")
                .replace(" ", "")
                .replace(",", ".")
                .toDouble()
            else -> throw IllegalArgumentException(
                "Unexpected cell type ${cell.cellType} for column '$columnName' in row ${row.rowNum}"
            )
        }
    }
}
