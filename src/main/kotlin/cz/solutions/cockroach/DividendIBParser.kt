package cz.solutions.cockroach

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object DividendIBParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd")
    private const val BROKER_NAME = "Interactive Brokers"
    private val NON_US_TICKER_SUFFIX = Regex("""\.([A-Z]{1,3})\b""")

    fun parse(file: File): DividendXlsxResult {
        return file.inputStream().use { parse(it) }
    }

    fun parse(inputStream: InputStream): DividendXlsxResult {
        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()

        val format = CSVFormat.Builder.create()
            .setDelimiter(',')
            .build()

        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
            CSVParser(reader, format).use { parser ->
                val records = parser.records
                val baseCurrency = records.firstOrNull {
                    it.hasValueAt(0, "Summary") && it.hasValueAt(1, "Data") &&
                            it.hasValueAt(2, "Base Currency")
                }?.get(3)?.trim()
                    ?: throw IllegalArgumentException("Interactive Brokers CSV has no Summary/Data/Base Currency row")
                require(baseCurrency == Currency.USD.name) {
                    "Interactive Brokers initial dividend support requires USD base currency, found '$baseCurrency'"
                }
                val transactionHeader = records.firstOrNull {
                    it.hasValueAt(0, "Transaction History") && it.hasValueAt(1, "Header")
                } ?: throw IllegalArgumentException("Interactive Brokers CSV has no Transaction History header row")
                mapOf(
                    2 to "Date",
                    5 to "Transaction Type",
                    6 to "Symbol",
                    10 to "Gross Amount",
                ).forEach { (index, expected) ->
                    require(transactionHeader.hasValueAt(index, expected)) {
                        val actual = if (transactionHeader.size() > index) transactionHeader.get(index).trim() else "<missing>"
                        "Interactive Brokers CSV column $index is '$actual', expected '$expected'"
                    }
                }

                records
                    .filter { it.hasValueAt(0, "Transaction History") && it.hasValueAt(1, "Data") }
                    .forEach { row ->
                        require(row.size() > 12) {
                            "Interactive Brokers Transaction History row has ${row.size()} columns, expected at least 13"
                        }
                        val transactionType = row.get(5).trim()
                        val date = LocalDate.parse(row.get(2).trim(), DATE_FORMATTER)
                        val symbol = row.get(6).trim()
                        val amount = row.get(10).trim().toDouble()

                        when (transactionType) {
                            "Dividend" -> {
                                require(symbol.isNotBlank()) { "Interactive Brokers dividend at $date has no symbol" }
                                require(NON_US_TICKER_SUFFIX.find(symbol) == null) {
                                    "Interactive Brokers ticker '$symbol' at $date looks non-US. " +
                                            "Initial support assumes US-source dividends; report this row manually."
                                }
                                require(amount > 0.0) {
                                    "Interactive Brokers dividend for $symbol at $date must be positive, was $amount"
                                }
                                dividends.add(
                                    DividendRecord(
                                        date,
                                        amount,
                                        Currency.USD,
                                        symbol = symbol,
                                        broker = BROKER_NAME,
                                        country = "US",
                                    )
                                )
                            }
                            "Foreign Tax Withholding" -> {
                                require(symbol.isNotBlank()) { "Interactive Brokers withholding at $date has no symbol" }
                                require(amount <= 0.0) {
                                    "Interactive Brokers withholding for $symbol at $date must be non-positive, was $amount"
                                }
                                taxes.add(TaxRecord(date, amount, Currency.USD, symbol = symbol, broker = BROKER_NAME))
                            }
                        }
                    }
            }
        }

        val taxedKeys = taxes.map { it.date to it.symbol }.toSet()
        dividends.distinctBy { it.date to it.symbol }
            .filter { (it.date to it.symbol) !in taxedKeys }
            .forEach {
                taxes.add(TaxRecord(it.date, 0.0, it.currency, symbol = it.symbol, broker = it.broker))
            }

        return DividendXlsxResult(dividends, taxes)
    }

    private fun CSVRecord.hasValueAt(index: Int, value: String): Boolean {
        return size() > index && get(index).trim() == value
    }
}
