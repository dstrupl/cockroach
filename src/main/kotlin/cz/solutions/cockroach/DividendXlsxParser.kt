package cz.solutions.cockroach

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.InputStream

data class DividendXlsxResult(
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>
)

object DividendXlsxParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy")

    fun parse(file: File): DividendXlsxResult {
        return file.inputStream().use { parse(it) }
    }

    private val IGNORED_DESCRIPTIONS = listOf("TREASURY LIQUIDITY FUND", "WIRE OUT")

    fun parse(inputStream: InputStream): DividendXlsxResult {
        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()

        XSSFWorkbook(inputStream).use { workbook ->
            val sheet = workbook.getSheetAt(0)

            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue

                val dateStr = row.getCell(0)?.stringCellValue?.trim() ?: continue
                val description = row.getCell(1)?.stringCellValue?.trim() ?: continue
                val value = row.getCell(2)?.numericCellValue ?: continue

                if (IGNORED_DESCRIPTIONS.any { description.contains(it, ignoreCase = true) }) continue

                val date = LocalDate.parse(dateStr, DATE_FORMATTER)

                if (description.contains("WITHHOLDING", ignoreCase = true)) {
                    taxes.add(TaxRecord(date, value))
                } else {
                    dividends.add(DividendRecord(date, value))
                }
            }
        }

        return DividendXlsxResult(dividends, taxes)
    }
}


