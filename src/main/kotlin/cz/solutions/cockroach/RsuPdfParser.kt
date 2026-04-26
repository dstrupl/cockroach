package cz.solutions.cockroach

import java.io.File

object RsuPdfParser {

    private const val BROKER_NAME = "Charles Schwab & Co."

    // After "Company Name (Symbol)" the symbol appears in parentheses, possibly on the next line.
    private val SYMBOL_PATTERN = Regex("""Company Name \(Symbol\)[\s\S]*?\(([A-Z][A-Z0-9.]*)\)""")

    /**
     * Parses a single RSU Release Confirmation PDF and returns an RsuRecord.
     */
    fun parse(pdfFile: File): RsuRecord {
        val text = PdfParserUtils.extractText(pdfFile)
        return parseFromText(text)
    }

    /**
     * Parses all RSU Release Confirmation PDFs in the given directory and returns a list of RsuRecords.
     */
    fun parseDirectory(directory: File): List<RsuRecord> {
        return PdfParserUtils.parseDirectory(directory, ::parse)
    }

    fun parseFromText(text: String): RsuRecord {
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
            broker = BROKER_NAME
        )
    }
}
