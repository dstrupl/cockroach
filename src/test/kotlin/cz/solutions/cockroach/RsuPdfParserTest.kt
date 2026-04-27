package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RsuPdfParserTest {

    private val brokerName = "Morgan Stanley & Co."

    private fun loadResourceAsFile(name: String): File {
        return File({}::class.java.getResource(name)!!.toURI())
    }

    @Test
    fun parsesReleaseConfirmationPdfText() {
        val text = """
             Release Summary
            Account Number 377167364
            Tax Payment Method Withhold Shares
            Company Name (Symbol) CISCO SYSTEMS, INC. (CSCO)
            Award Number 1623675
            Award Date 05-23-2023
            Award Type RSU
            Plan 05Release Date 12-10-2025
            Shares Released 53.0000
            Market Value Per Share $79.510000
            Award Price Per Share $0.000000
        """.trimIndent()

        val record = RsuPdfParser.parseFromText(text, brokerName)

        assertThat(record).isEqualTo(
            RsuRecord(
                date = LocalDate(2025, 12, 10),
                quantity = 53,
                vestFmv = 79.51,
                vestDate = LocalDate(2025, 12, 10),
                grantId = "1623675",
                symbol = "CSCO",
                broker = brokerName
            )
        )
    }

    @Test
    fun parsesAnotherReleaseConfirmationPdfText() {
        val text = """
             Release Summary
            Account Number 377167364
            Tax Payment Method Withhold Shares
            Company Name (Symbol) CISCO SYSTEMS, INC. - NEW
            (CSCO)
            Award Number 1679633
            Award Date 06-05-2024
            Award Type RSU
            Plan 05Release Date 09-10-2025
            Shares Released 18.0000
            Market Value Per Share $67.340000
            Award Price Per Share $0.000000
        """.trimIndent()

        val record = RsuPdfParser.parseFromText(text, brokerName)

        assertThat(record).isEqualTo(
            RsuRecord(
                date = LocalDate(2025, 9, 10),
                quantity = 18,
                vestFmv = 67.34,
                vestDate = LocalDate(2025, 9, 10),
                grantId = "1679633",
                symbol = "CSCO",
                broker = brokerName
            )
        )
    }

    @Test
    fun parsesSinglePdf() {
        val pdfFile = loadResourceAsFile("getReleaseConfirmation.pdf")

        val record = RsuPdfParser.parse(pdfFile, brokerName)

        assertThat(record).isEqualTo(
            RsuRecord(
                date = LocalDate(2025, 12, 10),
                quantity = 53,
                vestFmv = 79.51,
                vestDate = LocalDate(2025, 12, 10),
                grantId = "1623675",
                symbol = "CSCO",
                broker = brokerName
            )
        )
    }

    @Test
    fun parsesDirectory(@TempDir tempDir: File) {
        val pdfFile = loadResourceAsFile("getReleaseConfirmation.pdf")

        // Copy the same PDF multiple times to simulate a directory with several release confirmations
        pdfFile.copyTo(File(tempDir, "release1.pdf"))
        pdfFile.copyTo(File(tempDir, "release2.pdf"))
        pdfFile.copyTo(File(tempDir, "release3.pdf"))

        val records = RsuPdfParser.parseDirectory(tempDir, brokerName)

        assertThat(records).hasSize(3)
        assertThat(records).allSatisfy { record ->
            assertThat(record).isEqualTo(
                RsuRecord(
                    date = LocalDate(2025, 12, 10),
                    quantity = 53,
                    vestFmv = 79.51,
                    vestDate = LocalDate(2025, 12, 10),
                    grantId = "1623675",
                    symbol = "CSCO",
                    broker = brokerName
                )
            )
        }
    }

    @Test
    fun parsesDirectoryIgnoresNonPdfFiles(@TempDir tempDir: File) {
        val pdfFile = loadResourceAsFile("getReleaseConfirmation.pdf")

        pdfFile.copyTo(File(tempDir, "release1.pdf"))
        File(tempDir, "notes.txt").writeText("not a pdf")

        val records = RsuPdfParser.parseDirectory(tempDir, brokerName)

        assertThat(records).hasSize(1)
    }
}
