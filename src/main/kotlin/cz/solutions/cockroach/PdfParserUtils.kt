package cz.solutions.cockroach

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.io.File

object PdfParserUtils {

    val DATE_FORMATTER = DateTimeFormat.forPattern("MM-dd-yyyy")

    fun extractText(pdfFile: File, page: Int = 1): String {
        Loader.loadPDF(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            stripper.startPage = page
            stripper.endPage = page
            return stripper.getText(document)
        }
    }

    fun <T> parseDirectory(directory: File, parseSingle: (File) -> T): List<T> {
        if(!directory.exists()) {
            return emptyList()
        }
        require(directory.isDirectory) { "${directory.absolutePath} is not a directory" }
        return directory.listFiles { file -> file.extension.lowercase() == "pdf" }
            ?.sorted()
            ?.map { parseSingle(it) }
            ?: emptyList()
    }

    fun extractDate(text: String, label: String): LocalDate {
        val regex = Regex("""$label\s+(\d{2}-\d{2}-\d{4})""")
        val match = regex.find(text) ?: error("Could not find $label in PDF")
        return LocalDate.parse(match.groupValues[1], DATE_FORMATTER)
    }

    fun extractInt(text: String, label: String): Int {
        val regex = Regex("""$label\s+([\d.]+)""")
        val match = regex.find(text) ?: error("Could not find $label in PDF")
        return match.groupValues[1].toDouble().toInt()
    }

    fun extractDouble(text: String, label: String): Double {
        val regex = Regex("""$label\s+([\d.]+)""")
        val match = regex.find(text) ?: error("Could not find $label in PDF")
        return match.groupValues[1].toDouble()
    }

    fun extractDollarAmount(text: String, label: String): Double {
        val regex = Regex("""$label\s+\$([\d.]+)""")
        val match = regex.find(text) ?: error("Could not find $label in PDF")
        return match.groupValues[1].toDouble()
    }

    fun extractString(text: String, label: String): String {
        val regex = Regex("""$label\s+(\S+)""")
        val match = regex.find(text) ?: error("Could not find $label in PDF")
        return match.groupValues[1]
    }
}

