package cz.solutions.cockroach

object RsuReportPdfGenerator {

    fun generate(report: RsuReport, taxableMode: Boolean = false): ByteArray {
        val fmt = FormatingHelper::formatDouble

        val baseColumns = listOf(
            PdfColumn("Cenný papír", 1f), PdfColumn("Obchodník", 1.3f),
            PdfColumn("Datum připsání", 1f), PdfColumn("Počet akcií", 1f), PdfColumn("Tržní cena (USD)", 1f),
            PdfColumn("Zisk (USD)", 1f), PdfColumn("Kurz D54 (Kč/USD)", 1f), PdfColumn("Zisk (Kč)", 1f)
        )
        val extraColumns = if (taxableMode) listOf(
            PdfColumn("Prodáno a zdaněno", 1f), PdfColumn("Zdanitelný zisk (Kč)", 1f)
        ) else emptyList()
        val columns = baseColumns + extraColumns

        val rows = report.printableRsuList.map { r ->
            val base = listOf(r.symbol, r.broker, r.date, r.amount.toString(), r.onePriceDolarValue, r.vestDolarValue, r.exchange, r.vestCroneValue)
            if (taxableMode) base + listOf(r.soldAmount, r.taxableVestCroneValue) else base
        }

        val baseSummary = listOf(
            SummaryCell.empty(),                                // Cenný papír
            SummaryCell.empty(),                                // Obchodník
            SummaryCell.empty(),                                // Datum připsání
            SummaryCell.bold(report.totalAmount.toString()),    // Počet akcií
            SummaryCell.empty(),                                // Tržní cena (USD)
            SummaryCell.bold(fmt(report.rsuDollarValue)),       // Zisk (USD)
            SummaryCell.empty(),                                // Kurz D54
            SummaryCell.bold(fmt(report.rsuCroneValue))         // Zisk (Kč)
        )
        val extraSummary = if (taxableMode) listOf(
            SummaryCell.empty(),                                     // Prodáno a zdaněno
            SummaryCell.bold(fmt(report.taxableRsuCroneValue))       // Zdanitelný zisk (Kč)
        ) else emptyList()
        val summaryRow = baseSummary + extraSummary

        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Nepeněžní příjmy dle §6 ze zahraničí – akciový program pro zaměstnance (§6, odst. 3)",
            subtitles = listOf("Program RS – bezplatné poskytnutí akcií"),
            columns = columns, rows = rows, summaryRow = summaryRow,
            landscape = true
        ))
    }
}

