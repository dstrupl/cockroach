package cz.solutions.cockroach

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.InputStream
import java.util.logging.Logger

data class DegiroParseResult(
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>
)

object DegiroAccountStatementParser {

    private val LOGGER = Logger.getLogger(DegiroAccountStatementParser::class.java.name)

    private val DATE_FORMATTER = DateTimeFormat.forPattern("dd-MM-yyyy")

    private const val SHEET_NAME = "Přehled účtu"

    private const val DESC_DIVIDEND = "Dividenda"
    private const val DESC_TAX = "Daň z dividendy"
    private const val DESC_ADR_FEE = "ADR/GDR Pass-Through poplatek"

    private const val COL_VALUE_DATE = 2
    private const val COL_DESCRIPTION = 5
    private const val COL_CURRENCY = 7
    private const val COL_AMOUNT = 8

    fun parse(file: File): DegiroParseResult {
        return file.inputStream().use { parse(it) }
    }

    fun parse(inputStream: InputStream): DegiroParseResult {
        return WorkbookFactory.create(inputStream).use { parse(it) }
    }

    fun parse(workbook: Workbook): DegiroParseResult {
        val sheet = workbook.getSheet(SHEET_NAME)
            ?: throw IllegalArgumentException("Sheet '$SHEET_NAME' not found in Degiro statement")

        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()
        var ignoredAdrCount = 0
        var ignoredAdrTotal = 0.0
        var ignoredAdrCurrency: String? = null

        for (i in 1..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val description = stringCell(row, COL_DESCRIPTION) ?: continue
            when (description.trim()) {
                DESC_DIVIDEND -> {
                    val record = parseRecord(row) ?: continue
                    dividends.add(DividendRecord(record.date, record.amount, record.currency))
                }
                DESC_TAX -> {
                    val record = parseRecord(row) ?: continue
                    taxes.add(TaxRecord(record.date, record.amount, record.currency))
                }
                DESC_ADR_FEE -> {
                    val record = parseRecord(row) ?: continue
                    ignoredAdrCount++
                    ignoredAdrTotal += record.amount
                    ignoredAdrCurrency = record.currency.name
                }
            }
        }

        if (ignoredAdrCount > 0) {
            LOGGER.info("Ignored $ignoredAdrCount ADR/GDR Pass-Through fee row(s) totalling $ignoredAdrTotal $ignoredAdrCurrency (not a withholding tax).")
        }

        return DegiroParseResult(dividends, taxes)
    }

    private data class ParsedRow(val date: LocalDate, val amount: Double, val currency: Currency)

    private fun parseRecord(row: Row): ParsedRow? {
        val dateStr = stringCell(row, COL_VALUE_DATE) ?: return null
        val currencyStr = stringCell(row, COL_CURRENCY) ?: return null
        val amountStr = stringCell(row, COL_AMOUNT) ?: return null

        val date = LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        val currency = try {
            Currency.valueOf(currencyStr.trim())
        } catch (e: IllegalArgumentException) {
            LOGGER.warning("Unknown currency '$currencyStr' on row ${row.rowNum + 1}, skipping")
            return null
        }
        val amount = parseAmount(amountStr) ?: return null
        return ParsedRow(date, amount, currency)
    }

    private fun parseAmount(input: String): Double? {
        val cleaned = input.trim()
            .replace("\u00a0", "")
            .replace(" ", "")
            .replace(",", ".")
        return cleaned.toDoubleOrNull()
    }

    private fun stringCell(row: Row, index: Int): String? {
        val cell: Cell = row.getCell(index) ?: return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.takeIf { it.isNotBlank() }
            CellType.NUMERIC -> cell.numericCellValue.toString()
            else -> null
        }
    }
}
