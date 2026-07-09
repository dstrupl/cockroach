package cz.solutions.cockroach

import java.io.File

object EsppPdfParser {

    // After "Company Name (Symbol)" the symbol appears in parentheses, possibly on the next line.
    private val SYMBOL_PATTERN = Regex("""Company Name \(Symbol\)[\s\S]*?\(([A-Z][A-Z0-9.]*)\)""")

    /**
     * Parses a single ESPP Purchase Confirmation PDF and returns an EsppRecord stamped with [brokerName].
     * The PDF format itself does not identify the issuing broker (Schwab and E-Trade/Morgan Stanley both
     * deliver Schwab-style Purchase Confirmations), so the caller must supply the broker name.
     */
    fun parse(pdfFile: File, brokerName: String): EsppRecord {
        val text = PdfParserUtils.extractText(pdfFile)
        return parseFromText(text, brokerName)
    }

    /**
     * Parses all ESPP Purchase Confirmation PDFs in the given directory and returns a list of EsppRecords
     * stamped with [brokerName].
     */
    fun parseDirectory(directory: File, brokerName: String): List<EsppRecord> {
        return PdfParserUtils.parseDirectory(directory) { parse(it, brokerName) }
    }

    fun parseFromText(text: String, brokerName: String): EsppRecord {
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
            broker = brokerName
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

