package cz.solutions.cockroach

import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.multipdf.PDFMergerUtility
import java.io.ByteArrayOutputStream

object InterestReportPdfGenerator {

    fun generate(report: InterestReport): ByteArray {
        val pdfs = mutableListOf<ByteArray>()
        for (section in report.sections) {
            pdfs.add(generateCurrencySectionPdf(section))
        }
        for (section in report.czkSections) {
            pdfs.add(generateCzkSectionPdf(section))
        }

        if (pdfs.isEmpty()) {
            return PdfReportGenerator.generate(PdfReportDefinition(
                title = "Úroky (§8) – rozpis",
                subtitles = listOf("Žádné úrokové příjmy v daném období."),
                columns = listOf(PdfColumn("Datum", 1f)),
                rows = emptyList(),
                landscape = false
            ))
        }
        if (pdfs.size == 1) return pdfs[0]
        return mergePdfs(pdfs)
    }

    private fun generateCurrencySectionPdf(section: CurrencyInterestSection): ByteArray {
        val cur = section.currency.name
        val country = section.country
        val columns = listOf(
            PdfColumn("Produkt", 2f), PdfColumn("Obchodník", 1.5f),
            PdfColumn("Datum", 0.9f), PdfColumn("Brutto ($cur)", 0.9f), PdfColumn("Sražená daň ($cur)", 1.1f),
            PdfColumn("Kurz (Kč/$cur)", 1f), PdfColumn("Brutto (Kč)", 0.9f), PdfColumn("Sražená daň (Kč)", 1.1f)
        )
        val rows = section.printableInterestList.map { i ->
            listOf(i.product, i.broker, i.date, i.brutto, i.tax, i.exchange, i.bruttoCrown, i.taxCrown)
        }
        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.empty(),                                // Produkt
            SummaryCell.empty(),                                // Obchodník
            SummaryCell.bold("Celkem"),
            SummaryCell.bold(fmt(section.totalBrutto)),
            SummaryCell.bold(fmt(section.totalTax)),
            SummaryCell.empty(),
            SummaryCell.bold(fmt(section.totalBruttoCrown)),
            SummaryCell.bold(fmt(section.totalTaxCrown)),
        )
        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Úroky (§8) – rozpis – $country / $cur",
            subtitles = listOf("Země zdroje: $country", "Měna zdroje: $cur"),
            columns = columns, rows = rows, summaryRow = summaryRow,
            landscape = false
        ))
    }

    private fun generateCzkSectionPdf(section: CzkInterestSection): ByteArray {
        val country = section.country
        val columns = listOf(
            PdfColumn("Produkt", 2f), PdfColumn("Obchodník", 1.5f),
            PdfColumn("Datum", 1f), PdfColumn("Brutto (Kč)", 1f), PdfColumn("Sražená daň (Kč)", 1.1f)
        )
        val rows = section.printableInterestList.map { i -> listOf(i.product, i.broker, i.date, i.brutto, i.tax) }
        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.empty(),                                // Produkt
            SummaryCell.empty(),                                // Obchodník
            SummaryCell.bold("Celkem"),
            SummaryCell.bold(fmt(section.totalBruttoCrown)),
            SummaryCell.bold(fmt(section.totalTaxCrown)),
        )
        val title = if (country == "CZ") "Úroky ze zdrojů v ČR – rozpis" else "Úroky (§8) – rozpis – $country / CZK"
        return PdfReportGenerator.generate(PdfReportDefinition(
            title = title,
            subtitles = listOf("Země zdroje: $country", "Měna zdroje: CZK"),
            columns = columns, rows = rows, summaryRow = summaryRow,
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
