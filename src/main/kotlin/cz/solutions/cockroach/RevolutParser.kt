package cz.solutions.cockroach

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.logging.Logger

data class RevolutStocksParseResult(
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>
)

data class RevolutSavingsParseResult(
    val interestRecords: List<InterestRecord>
)

object RevolutParser {

    private val LOGGER = Logger.getLogger(RevolutParser::class.java.name)

    const val DEFAULT_WHT_RATE = 0.15

    private const val STOCKS_TYPE_DIVIDEND = "DIVIDEND"
    private const val STOCKS_TYPE_DIVIDEND_TAX_CORRECTION = "DIVIDEND TAX (CORRECTION)"

    private val SAVINGS_DATE_FORMATTER = DateTimeFormat.forPattern("MMM d, yyyy, h:mm:ss a").withLocale(Locale.US)

    fun parseStocks(file: File, whtRate: Double = DEFAULT_WHT_RATE): RevolutStocksParseResult {
        return file.reader(StandardCharsets.UTF_8).use { parseStocks(it, whtRate) }
    }

    fun parseStocks(reader: Reader, whtRate: Double = DEFAULT_WHT_RATE): RevolutStocksParseResult {
        require(whtRate in 0.0..0.99) { "whtRate must be in [0, 0.99), was $whtRate" }
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader().setSkipHeaderRecord(true).build()
        val dividends = mutableListOf<DividendRecord>()
        val taxes = mutableListOf<TaxRecord>()
        var correctionPairTotal = 0.0
        var correctionRowCount = 0

        CSVParser.parse(reader, format).use { parser ->
            for (record in parser) {
                val type = record.get("Type").trim()
                when (type) {
                    STOCKS_TYPE_DIVIDEND -> {
                        val date = parseStocksDate(record.get("Date"))
                        val (amount, currency) = parseAmountAndCurrency(record)
                        if (amount <= 0.0) {
                            LOGGER.warning("Revolut Stocks: skipping non-positive DIVIDEND row at $date (${record.get("Total Amount")})")
                            continue
                        }
                        val gross = amount / (1.0 - whtRate)
                        val wht = gross - amount
                        dividends.add(DividendRecord(date, gross, currency))
                        if (wht > 0.0) {
                            taxes.add(TaxRecord(date, -wht, currency))
                        }
                    }
                    STOCKS_TYPE_DIVIDEND_TAX_CORRECTION -> {
                        val (amount, _) = parseAmountAndCurrency(record)
                        correctionPairTotal += amount
                        correctionRowCount++
                    }
                    else -> { /* CASH WITHDRAWAL, BUY, SELL, ... ignored */ }
                }
            }
        }

        if (correctionRowCount > 0) {
            if (Math.abs(correctionPairTotal) < 0.01) {
                LOGGER.info("Revolut Stocks: $correctionRowCount DIVIDEND TAX (CORRECTION) row(s) summed to ${"%.2f".format(correctionPairTotal)} (cancelling pairs); ignored.")
            } else {
                LOGGER.warning("Revolut Stocks: $correctionRowCount DIVIDEND TAX (CORRECTION) row(s) summed to ${"%.2f".format(correctionPairTotal)} (non-zero net); ignored - inspect statement manually.")
            }
        }
        LOGGER.info("Revolut Stocks: parsed ${dividends.size} dividend(s); grossed-up at WHT rate ${"%.4f".format(whtRate)} producing ${taxes.size} tax record(s).")
        return RevolutStocksParseResult(dividends, taxes)
    }

    fun parseSavings(file: File): RevolutSavingsParseResult {
        return file.reader(StandardCharsets.UTF_8).use { parseSavings(it) }
    }

    fun parseSavings(reader: Reader): RevolutSavingsParseResult {
        val format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader().setSkipHeaderRecord(true).build()
        val interestRecords = mutableListOf<InterestRecord>()
        var feeTotal = 0.0
        var feeCount = 0
        var feeCurrency: Currency? = null

        CSVParser.parse(reader, format).use { parser ->
            val valueColumn = parser.headerNames.firstOrNull { it.startsWith("Value, ") && it != "Value, CZK" }
                ?: throw IllegalArgumentException("Revolut Savings: no 'Value, <CCY>' column found in header ${parser.headerNames}")
            val currency = Currency.valueOf(valueColumn.removePrefix("Value, ").trim())

            for (record in parser) {
                val description = record.get("Description").trim()
                val rawValue = record.get(valueColumn).trim()
                if (rawValue.isEmpty()) continue
                val value = rawValue.replace(",", "").toDoubleOrNull() ?: continue
                val date = parseSavingsDate(record.get("Date"))
                when {
                    description.startsWith("Interest PAID") -> {
                        if (value > 0.0) interestRecords.add(InterestRecord(date, value, currency))
                    }
                    description.startsWith("Service Fee Charged") -> {
                        feeTotal += value
                        feeCount++
                        feeCurrency = currency
                    }
                    else -> { /* Interest Reinvested, BUY, SELL: ignored */ }
                }
            }
        }
        if (feeCount > 0) {
            LOGGER.info("Revolut Savings: ignored $feeCount Service Fee Charged row(s) totalling ${"%.4f".format(feeTotal)} ${feeCurrency?.name} (informational only; not deductible from §8 interest base per Revolut CZ tax guidance).")
        }
        LOGGER.info("Revolut Savings: parsed ${interestRecords.size} Interest PAID row(s) as gross §8 interest income.")
        return RevolutSavingsParseResult(interestRecords)
    }

    private fun parseStocksDate(value: String): LocalDate {
        return LocalDate.parse(value.trim().substring(0, 10))
    }

    private fun parseSavingsDate(value: String): LocalDate {
        // "Dec 31, 2025, 1:51:12 AM" — Revolut uses U+202F (narrow no-break space) and/or
        // U+00A0 (non-breaking space) around the AM/PM marker; normalise to ASCII space first.
        val normalised = value.trim().replace('\u202F', ' ').replace('\u00A0', ' ')
        return SAVINGS_DATE_FORMATTER.parseLocalDate(normalised)
    }

    private fun parseAmountAndCurrency(record: CSVRecord): Pair<Double, Currency> {
        val raw = record.get("Total Amount").trim()
        val currencyStr = record.get("Currency").trim()
        val currency = Currency.valueOf(currencyStr)
        val numericPart = raw.removePrefix(currencyStr).trim().replace(",", "")
        val amount = numericPart.toDouble()
        return amount to currency
    }
}
