package cz.solutions.cockroach

import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.multipdf.PDFMergerUtility
import java.io.ByteArrayOutputStream

object DividendReportPdfGenerator {

    fun generate(report: DividendReport): ByteArray {
        val pdfs = mutableListOf<ByteArray>()
        for (section in report.sections) {
            pdfs.add(generateCurrencySectionPdf(section))
        }
        report.czkSection?.let { pdfs.add(generateCzkSectionPdf(it)) }

        if (pdfs.isEmpty()) {
            return PdfReportGenerator.generate(PdfReportDefinition(
                title = "Dividendy (§8) – rozpis",
                subtitles = listOf("Žádné dividendy v daném období."),
                columns = listOf(PdfColumn("Datum", 1f)),
                rows = emptyList(),
                landscape = false
            ))
        }
        if (pdfs.size == 1) return pdfs[0]
        return mergePdfs(pdfs)
    }

    private fun generateCurrencySectionPdf(section: CurrencyDividendSection): ByteArray {
        val cur = section.currency.name
        val columns = listOf(
            PdfColumn("Datum", 1f), PdfColumn("Brutto ($cur)", 1f), PdfColumn("Sražená daň ($cur)", 1f),
            PdfColumn("Kurz (Kč/$cur)", 1f), PdfColumn("Brutto (Kč)", 1f), PdfColumn("Sražená daň (Kč)", 1f)
        )
        val rows = section.printableDividendList.map { d ->
            listOf(d.date, d.brutto, d.tax, d.exchange, d.bruttoCrown, d.taxCrown)
        }
        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.bold("Celkem"),
            SummaryCell.bold(fmt(section.totalBrutto)),
            SummaryCell.bold(fmt(section.totalTax)),
            SummaryCell.empty(),
            SummaryCell.bold(fmt(section.totalBruttoCrown)),
            SummaryCell.bold(fmt(section.totalTaxCrown))
        )
        val footerLines = mutableListOf<String>()
        if (section.totalTaxReversal > 0) {
            footerLines.add("Vrácená daň (tax reversal): ${fmt(section.totalTaxReversalCrown)} CZK (${fmt(section.totalTaxReversal)} $cur)")
        }
        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Dividendy (§8) – rozpis – $cur",
            subtitles = listOf("Měna zdroje: $cur"),
            columns = columns, rows = rows, summaryRow = summaryRow, footerLines = footerLines,
            landscape = false
        ))
    }

    private fun generateCzkSectionPdf(section: CzkDividendSection): ByteArray {
        val columns = listOf(
            PdfColumn("Datum", 1f), PdfColumn("Brutto (Kč)", 1f), PdfColumn("Sražená daň (Kč)", 1f)
        )
        val rows = section.printableDividendList.map { d -> listOf(d.date, d.brutto, d.tax) }
        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.bold("Celkem"),
            SummaryCell.bold(fmt(section.totalBruttoCrown)),
            SummaryCell.bold(fmt(section.totalTaxCrown))
        )
        val footerLines = mutableListOf<String>()
        if (section.totalTaxReversalCrown > 0) {
            footerLines.add("Vrácená daň (tax reversal): ${fmt(section.totalTaxReversalCrown)} CZK")
        }
        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Dividendy ze zdrojů v ČR – rozpis",
            subtitles = listOf("Měna zdroje: CZK"),
            columns = columns, rows = rows, summaryRow = summaryRow, footerLines = footerLines,
            landscape = false
        ))
    }

    private fun mergePdfs(pdfs: List<ByteArray>): ByteArray {
        val merger = PDFMergerUtility()
        val out = ByteArrayOutputStream()
        merger.destinationStream = out
        for (pdf in pdfs) merger.addSource(RandomAccessReadBuffer(pdf))
        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache())
        return out.toByteArray()
    }
}


