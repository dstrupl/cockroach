package cz.solutions.cockroach

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.LocalDate
import java.io.File
import java.util.logging.Logger

/**
 * Parses a VÚB CZK account statement PDF and extracts every "Credit interest"
 * (or Slovak "Úroky pripísané") posting as an [InterestRecord].
 *
 * VÚB exports the table as plain text columns. After [PDFTextStripper] each
 * posting is rendered as four consecutive lines:
 *
 *   <booking-date> [<value-date>] <currency-marker>
 *   <transaction-reference>             // e.g. "0201IG0013829"
 *   <amount>                            // Czech format: "125,23" or "1.234,56"
 *   Credit interest                     // anchor label
 *
 * The label is used as the anchor; the three preceding lines provide the
 * value date (the last DD/MM token, which matches the reference prefix), the
 * IG-style reference, and the amount in CZK. Non-standard rows whose
 * reference does not match the IG pattern are skipped with a warning.
 */
object VubInterestPdfParser {

    private val LOGGER = Logger.getLogger(VubInterestPdfParser::class.java.name)
    private const val BROKER_NAME = "VÚB"
    private const val COUNTRY = "SK"
    private val LABELS = listOf("Credit interest", "Úroky pripísané")
    private val DATE_TOKEN = Regex("""\b(\d{2})/(\d{2})\b""")
    private val IG_REFERENCE = Regex("""^\d{4}IG\d+$""")
    private val IBAN_IN_FILENAME = Regex("""(SK\d{22})""")

    fun parse(file: File, year: Int): List<InterestRecord> {
        Loader.loadPDF(file).use { doc ->
            val stripper = PDFTextStripper()
            stripper.startPage = 1
            stripper.endPage = doc.numberOfPages
            val text = stripper.getText(doc)
            require(text.contains("Currency: CZK", ignoreCase = true)) {
                "VÚB statement ${file.name} does not declare 'Currency: CZK' – non-CZK accounts are not supported."
            }
            val product = extractProduct(file, text)
            return parseText(text, year, product, file.name)
        }
    }

    private fun parseText(text: String, year: Int, product: String, fileName: String): List<InterestRecord> {
        val lines = text.lines()
        val records = mutableListOf<InterestRecord>()
        var skipped = 0

        for (i in lines.indices) {
            val label = lines[i].trim()
            if (LABELS.none { it.equals(label, ignoreCase = true) }) continue
            if (i < 3) {
                skipped++; continue
            }
            val dateLine = lines[i - 3].trim()
            val refLine = lines[i - 2].trim()
            val amountLine = lines[i - 1].trim()

            if (!IG_REFERENCE.matches(refLine)) {
                LOGGER.warning("VÚB $fileName: skipping non-standard 'Credit interest' near line ${i + 1} (reference='$refLine')")
                skipped++; continue
            }
            val date = lastDateToken(dateLine, year)
            if (date == null) {
                LOGGER.warning("VÚB $fileName: cannot parse date from '$dateLine' near line ${i + 1}; skipping")
                skipped++; continue
            }
            val amount = parseAmount(amountLine)
            if (amount == null) {
                LOGGER.warning("VÚB $fileName: cannot parse amount from '$amountLine' near line ${i + 1}; skipping")
                skipped++; continue
            }
            if (amount <= 0.0) {
                skipped++; continue
            }
            records.add(InterestRecord(date, amount, Currency.CZK, product = product, broker = BROKER_NAME, country = COUNTRY))
        }
        LOGGER.info("VÚB: parsed ${records.size} interest record(s) from $fileName (skipped=$skipped)")
        return records
    }

    private fun lastDateToken(line: String, year: Int): LocalDate? {
        val matches = DATE_TOKEN.findAll(line).toList()
        if (matches.isEmpty()) return null
        val last = matches.last()
        val day = last.groupValues[1].toInt()
        val month = last.groupValues[2].toInt()
        return try {
            LocalDate(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAmount(line: String): Double? {
        // Czech format: "1.234,56" – '.' is thousands separator, ',' is decimal.
        val cleaned = line.replace("\u00a0", "").replace(" ", "")
            .replace(".", "").replace(",", ".")
        return cleaned.toDoubleOrNull()
    }

    private fun extractProduct(file: File, text: String): String {
        IBAN_IN_FILENAME.find(file.name)?.let { return it.groupValues[1] }
        IBAN_IN_FILENAME.find(text)?.let { return it.groupValues[1] }
        return file.nameWithoutExtension
    }
}
