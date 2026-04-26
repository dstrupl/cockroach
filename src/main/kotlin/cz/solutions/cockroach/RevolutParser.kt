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

    private const val BROKER_NAME = "Revolut"

    /**
     * Country of origin for Revolut Savings interest. Flexible Account interest is paid by Irish-domiciled
     * UCITS money-market funds (ISINs `IE000H9J0QX4`, `IE000AZVL3K0`, ...), so the source country reported
     * on Příloha č. 3 is Ireland regardless of the cash currency (USD/EUR Class shares).
     */
    private const val SAVINGS_COUNTRY = "IE"

    private val SAVINGS_DATE_FORMATTER = DateTimeFormat.forPattern("MMM d, yyyy, h:mm:ss a").withLocale(Locale.US)

    // ISIN: 2-letter country code + 9 alphanumeric chars + 1 check digit (12 chars total).
    private val ISIN_PATTERN = Regex("\\b([A-Z]{2}[A-Z0-9]{9}[0-9])\\b")

    // Safety net for parseSavings: any unhandled description containing one of these tokens
    // would indicate Revolut started withholding tax on Flexible Accounts (e.g. fund domicile change
    // or regulatory shift). Fail loudly so we never silently under-report §8 income.
    private val SAVINGS_TAX_PATTERN = Regex("(?i)\\b(WHT|withholding|tax\\s+(?:withheld|deducted|paid|charged))\\b")

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
                        val ticker = record.get("Ticker").trim()
                        // Revolut Stocks supports US-listed shares only, so the ISIN prefix is always "US".
                        dividends.add(DividendRecord(date, gross, currency, symbol = ticker, broker = BROKER_NAME, country = "US"))
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
                if (!rawValue.isEmpty()){
                    val value = rawValue.replace(",", "").toDoubleOrNull() ?: continue
                    val date = parseSavingsDate(record.get("Date"))
                    when {
                        description.startsWith("Interest PAID") -> {
                            if (value > 0.0) {
                                val product = ISIN_PATTERN.find(description)?.value ?: description
                                interestRecords.add(InterestRecord(date, value, currency, product = product, broker = BROKER_NAME, tax = 0.0, country = SAVINGS_COUNTRY))
                            }
                        }
                        description.startsWith("Service Fee Charged") -> {
                            feeTotal += value
                            feeCount++
                            feeCurrency = currency
                        }
                        description.startsWith("Interest Reinvested") -> { /* purely informational; cash already counted via Interest PAID */ }
                        description.startsWith("BUY") || description.startsWith("SELL") -> { /* fund-unit movements */ }
                        else -> {
                            check(!SAVINGS_TAX_PATTERN.containsMatchIn(description)) {
                                "Revolut Savings: encountered tax-related row '$description' at $date. " +
                                        "Parser assumes Flexible Account interest is gross (Irish UCITS, no WHT). " +
                                        "Investigate the statement manually before re-running."
                            }
                            LOGGER.warning("Revolut Savings: unrecognised row '$description' at $date; ignored.")
                        }
                    }
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
