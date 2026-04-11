package cz.solutions.cockroach

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object ETradeGainLossParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy")

    fun parse(data: String): List<SaleRecord> {
        val format = CSVFormat.Builder.create()
            .setDelimiter(';')
            .build()

        val parser = CSVParser.parse(data, format)
        val records = parser.records

        val dataRows = records.filter { it.get(0) == "Sell" }

        return  dataRows.map { parseSaleRecord(it) }
    }

    private fun parseSaleRecord(row: CSVRecord): SaleRecord {
        val dateSold = parseDate(row.get(12))
        val quantity = row.get(3).toDouble()
        val vestDate = parseDate(row.get(41))
        val adjustedCostBasisPerShare = parseCurrency(row.get(11))
        val proceedsPerShare = parseCurrency(row.get(14))
        val grantNumber = row.get(39)

        return SaleRecord(
            date = dateSold,
            type = "RS",
            quantity = quantity,
            salePrice = proceedsPerShare,
            purchasePrice = adjustedCostBasisPerShare,
            purchaseFmv = adjustedCostBasisPerShare,
            purchaseDate = vestDate,
            grantId = grantNumber
        )
    }



    private fun parseDate(value: String): LocalDate {
        return LocalDate.parse(value.trim(), DATE_FORMATTER)
    }

    private fun parseCurrency(value: String): Double {
        return value
            .replace("$", "")
            .replace("\u00a0", "")
            .replace(" ", "")
            .replace(",", ".")
            .toDouble()
    }
}
