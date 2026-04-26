package cz.solutions.cockroach

import java.io.File

object EsppPdfParser {

    private const val BROKER_NAME = "Charles Schwab & Co."

    // After "Company Name (Symbol)" the symbol appears in parentheses, possibly on the next line.
    private val SYMBOL_PATTERN = Regex("""Company Name \(Symbol\)[\s\S]*?\(([A-Z][A-Z0-9.]*)\)""")

    /**
     * Parses a single ESPP Purchase Confirmation PDF and returns an EsppRecord.
     */
    fun parse(pdfFile: File): EsppRecord {
        val text = PdfParserUtils.extractText(pdfFile)
        return parseFromText(text)
    }

    /**
     * Parses all ESPP Purchase Confirmation PDFs in the given directory and returns a list of EsppRecords.
     */
    fun parseDirectory(directory: File): List<EsppRecord> {
        return PdfParserUtils.parseDirectory(directory, ::parse)
    }

    fun parseFromText(text: String): EsppRecord {
        // "Purchase Date 12-31-2025Shares Purchased..." due to column merge
        val purchaseDate = PdfParserUtils.extractDate(text, "Purchase Date")
        val sharesPurchased = PdfParserUtils.extractDouble(text, "Shares Purchased")
        val purchasePricePerShare = extractPurchasePricePerShare(text)
        val grantDateMarketValue = PdfParserUtils.extractDollarAmount(text, "Grant Date Market Value")
        val purchaseValuePerShare = PdfParserUtils.extractDollarAmount(text, "Purchase Value per Share")
        val symbol = SYMBOL_PATTERN.find(text)?.groupValues?.get(1).orEmpty()

        return EsppRecord(
            date = purchaseDate,
            quantity = sharesPurchased,
            purchasePrice = purchasePricePerShare,
            subscriptionFmv = grantDateMarketValue,
            purchaseFmv = purchaseValuePerShare,
            purchaseDate = purchaseDate,
            symbol = symbol,
            broker = BROKER_NAME
        )
    }

    private fun extractPurchasePricePerShare(text: String): Double {
        // Purchase Price per Share is on its own line, followed by
        //         (85.000% of $47.520000) $40.392000
        val regex = Regex("""Purchase Price per Share\s+\([^)]+\)\s+\$([\d.]+)""")
        val match = regex.find(text)
            // fallback: single-line format
            ?: Regex("""Purchase Price per Share\s+\$([\d.]+)""").find(text)
            ?: error("Could not find Purchase Price per Share in PDF")
        return match.groupValues[1].toDouble()
    }
}

