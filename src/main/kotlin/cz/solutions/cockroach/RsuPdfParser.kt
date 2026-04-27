package cz.solutions.cockroach

import java.io.File

object RsuPdfParser {

    // After "Company Name (Symbol)" the symbol appears in parentheses, possibly on the next line.
    private val SYMBOL_PATTERN = Regex("""Company Name \(Symbol\)[\s\S]*?\(([A-Z][A-Z0-9.]*)\)""")

    /**
     * Parses a single RSU Release Confirmation PDF and returns an RsuRecord stamped with [brokerName].
     * The PDF format itself does not identify the issuing broker (Schwab and E-Trade/Morgan Stanley both
     * deliver Schwab-style Release Confirmations), so the caller must supply the broker name.
     */
    fun parse(pdfFile: File, brokerName: String): RsuRecord {
        val text = PdfParserUtils.extractText(pdfFile)
        return parseFromText(text, brokerName)
    }

    /**
     * Parses all RSU Release Confirmation PDFs in the given directory and returns a list of RsuRecords
     * stamped with [brokerName].
     */
    fun parseDirectory(directory: File, brokerName: String): List<RsuRecord> {
        return PdfParserUtils.parseDirectory(directory) { parse(it, brokerName) }
    }

    fun parseFromText(text: String, brokerName: String): RsuRecord {
        // The PDF text has "Plan 05Release Date MM-dd-yyyy" due to column merge
        val releaseDate = PdfParserUtils.extractDate(text, "Release Date")
        val sharesReleased = PdfParserUtils.extractInt(text, "Shares Released")
        val marketValuePerShare = PdfParserUtils.extractDollarAmount(text, "Market Value Per Share")
        val awardNumber = PdfParserUtils.extractString(text, "Award Number")
        val symbol = SYMBOL_PATTERN.find(text)?.groupValues?.get(1).orEmpty()

        return RsuRecord(
            date = releaseDate,
            quantity = sharesReleased,
            vestFmv = marketValuePerShare,
            vestDate = releaseDate,
            grantId = awardNumber,
            symbol = symbol,
            broker = brokerName
        )
    }
}
