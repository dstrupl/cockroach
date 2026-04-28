package cz.solutions.cockroach

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

object DividendIBParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")

    fun parse(file: File): DividendXlsxResult {
        return file.inputStream().use { parse(it) }
    }

    fun parse(file: FileInputStream): DividendXlsxResult {
        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()

        val format = CSVFormat.Builder.create()
            .setDelimiter(',')
            .build()

        InputStreamReader(file).use { reader ->
            CSVParser(reader, format).use { parser ->
                parser.records
                    .filter { it.hasValueAt(0, "Transaction History") && it.hasValueAt(1, "Data") }
                    .forEach { row ->
                        val transactionType = row.get(5).trim()
                        val date = LocalDate.parse(row.get(2).trim(), DATE_FORMATTER)
                        val amount = row.get(12).trim().toDouble()

                        when (transactionType) {
                            "Dividend" -> dividends.add(DividendRecord(date, amount))
                            "Foreign Tax Withholding" -> taxes.add(TaxRecord(date, amount))
                        }
                    }
            }
        }

        return DividendXlsxResult(dividends, taxes)
    }

    private fun CSVRecord.hasValueAt(index: Int, value: String): Boolean {
        return size() > index && get(index).trim() == value
    }
}