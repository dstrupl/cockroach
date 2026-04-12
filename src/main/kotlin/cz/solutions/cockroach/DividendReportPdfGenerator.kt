package cz.solutions.cockroach

object DividendReportPdfGenerator {

    fun generate(report: DividendReport): ByteArray {
        val columns = listOf(
            PdfColumn("Datum", 1f), PdfColumn("Brutto (USD)", 1f), PdfColumn("Sražená daň (USD)", 1f),
            PdfColumn("Kurz D54 (Kč/USD)", 1f), PdfColumn("Brutto (Kč)", 1f), PdfColumn("Sražená daň (Kč)", 1f)
        )

        val rows = report.printableDividendList.map { d ->
            listOf(d.date, d.bruttoDollar, d.taxDollar, d.exchange, d.bruttoCrown, d.taxCrown)
        }

        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.bold("Celkem"),       // Datum
            SummaryCell.bold(fmt(report.totalBruttoDollar)),  // Brutto (USD)
            SummaryCell.bold(fmt(report.totalTaxDollar)),     // Sražená daň (USD)
            SummaryCell.empty(),                               // Kurz D54
            SummaryCell.bold(fmt(report.totalBruttoCrown)),   // Brutto (Kč)
            SummaryCell.bold(fmt(report.totalTaxCrown))       // Sražená daň (Kč)
        )

        val footerLines = mutableListOf<String>()
        if (report.totalTaxReversalDollar > 0) {
            footerLines.add("Vrácená daň (tax reversal): ${fmt(report.totalTaxReversalCrown)} CZK (${fmt(report.totalTaxReversalDollar)} USD)")
        }

        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Dividendy (§8) – rozpis",
            subtitles = listOf("Cenný papír: Cisco Systems", "Stát zdroje příjmů: USA", "Obchodník: Charles Schwab & Co., Morgan Stanley & Co."),
            columns = columns, rows = rows, summaryRow = summaryRow, footerLines = footerLines,
            landscape = false
        ))
    }
}

