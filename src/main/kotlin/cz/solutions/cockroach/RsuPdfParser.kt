package cz.solutions.cockroach

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File

object RsuPdfParser {

    private val DATE_FORMATTER = DateTimeFormat.forPattern("MM-dd-yyyy")

    /**
     * Parses a single RSU Release Confirmation PDF and returns an RsuRecord.
     */
    fun parse(pdfFile: File): RsuRecord {
        val text = extractText(pdfFile)
        return parseFromText(text)
    }

    /**
     * Parses all RSU Release Confirmation PDFs in the given directory and returns a list of RsuRecords.
     */
    fun parseDirectory(directory: File): List<RsuRecord> {
        require(directory.isDirectory) { "${directory.absolutePath} is not a directory" }
        return directory.listFiles { file -> file.extension.lowercase() == "pdf" }
            ?.sorted()
            ?.map { parse(it) }
            ?: emptyList()
    }

    private fun extractText(pdfFile: File): String {
        Loader.loadPDF(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            stripper.startPage = 1
            stripper.endPage = 1
            return stripper.getText(document)
        }
    }

    internal fun parseFromText(text: String): RsuRecord {
        val releaseDate = extractReleaseDate(text)
        val sharesReleased = extractSharesReleased(text)
        val marketValuePerShare = extractMarketValuePerShare(text)
        val awardNumber = extractAwardNumber(text)

        return RsuRecord(
            date = releaseDate,
            quantity = sharesReleased,
            vestFmv = marketValuePerShare,
            vestDate = releaseDate,
            grantId = awardNumber
        )
    }

    private fun extractReleaseDate(text: String): LocalDate {
        // The PDF text has "Plan 05Release Date MM-dd-yyyy" due to column merge
        val regex = Regex("""Release Date\s+(\d{2}-\d{2}-\d{4})""")
        val match = regex.find(text) ?: error("Could not find Release Date in PDF")
        return LocalDate.parse(match.groupValues[1], DATE_FORMATTER)
    }

    private fun extractSharesReleased(text: String): Int {
        val regex = Regex("""Shares Released\s+([\d.]+)""")
        val match = regex.find(text) ?: error("Could not find Shares Released in PDF")
        return match.groupValues[1].toDouble().toInt()
    }

    private fun extractMarketValuePerShare(text: String): Double {
        val regex = Regex("""Market Value Per Share\s+\$([\d.]+)""")
        val match = regex.find(text) ?: error("Could not find Market Value Per Share in PDF")
        return match.groupValues[1].toDouble()
    }

    private fun extractAwardNumber(text: String): String {
        val regex = Regex("""Award Number\s+(\d+)""")
        val match = regex.find(text) ?: error("Could not find Award Number in PDF")
        return match.groupValues[1]
    }
}

