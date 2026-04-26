package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class EsppPdfParserTest {

    companion object {
        private val EXTERNAL_PDF = File("/Users/jandryse/Documents/dane/2026/input/e-trade/espp/getEsppConfirmation.pdf")
    }

    @Test
    fun parsesPurchaseConfirmationPdfText() {
        val text = """
             Purchase Summary
            Account Number 377167364
            Company Name (Symbol) CISCO SYSTEMS, INC.(CSCO)
            Plan ESPP
            Grant Date 07-01-2024
            Purchase Begin Date 07-01-2025
            Purchase Date 12-31-2025Shares Purchased to Date in Current Offering
            Beginning Balance 211.0000
            Shares Purchased 133.6870
            Total shares Purchased for Offering 344.6870
             Purchase Details
            Calculation of Shares Purchased 
            Grant Date Market Value $47.520000
            Purchase Value per Share $77.030000
            Purchase Price per Share 
                    (85.000% of $47.520000) $40.392000
        """.trimIndent()

        val record = EsppPdfParser.parseFromText(text)

        assertThat(record).isEqualTo(
            EsppRecord(
                date = LocalDate(2025, 12, 31),
                quantity = 133.687,
                purchasePrice = 40.392,
                subscriptionFmv = 47.52,
                purchaseFmv = 77.03,
                purchaseDate = LocalDate(2025, 12, 31),
                symbol = "CSCO",
                broker = "Charles Schwab & Co."
            )
        )
    }

    @Test
    fun parsesPurchaseConfirmationWithDifferentValues() {
        val text = """
             Purchase Summary
            Account Number 123456789
            Company Name (Symbol) ACME CORP.(ACME)
            Plan ESPP
            Grant Date 01-01-2025
            Purchase Begin Date 01-01-2025
            Purchase Date 06-30-2025Shares Purchased to Date in Current Offering
            Beginning Balance 100.0000
            Shares Purchased 50.1234
            Total shares Purchased for Offering 150.1234
             Purchase Details
            Calculation of Shares Purchased 
            Grant Date Market Value $55.000000
            Purchase Value per Share $65.500000
            Purchase Price per Share 
                    (85.000% of $55.000000) $46.750000
        """.trimIndent()

        val record = EsppPdfParser.parseFromText(text)

        assertThat(record).isEqualTo(
            EsppRecord(
                date = LocalDate(2025, 6, 30),
                quantity = 50.1234,
                purchasePrice = 46.75,
                subscriptionFmv = 55.0,
                purchaseFmv = 65.5,
                purchaseDate = LocalDate(2025, 6, 30),
                symbol = "ACME",
                broker = "Charles Schwab & Co."
            )
        )
    }

    @Test
    fun parsesActualPdf() {
        assumeTrue(EXTERNAL_PDF.exists(), "External PDF not available, skipping")

        val record = EsppPdfParser.parse(EXTERNAL_PDF)

        assertThat(record).isEqualTo(
            EsppRecord(
                date = LocalDate(2025, 12, 31),
                quantity = 133.687,
                purchasePrice = 40.392,
                subscriptionFmv = 47.52,
                purchaseFmv = 77.03,
                purchaseDate = LocalDate(2025, 12, 31),
                symbol = "CSCO",
                broker = "Charles Schwab & Co."
            )
        )
    }
}
