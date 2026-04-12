package cz.solutions.cockroach

object EsppReportPdfGenerator {

    fun generate(report: EsppReport, taxableMode: Boolean = false, broker: String = "Charles Schwab & Co., Morgan Stanley & Co."): ByteArray {
        val fmt = FormatingHelper::formatDouble

        val baseColumns = listOf(
            PdfColumn("Datum nákupu", 1f), PdfColumn("Počet akcií", 1f), PdfColumn("Zvýh. nákup. cena (USD)", 1.3f),
            PdfColumn("Tržní cena (USD)", 1f), PdfColumn("Zisk (USD)", 1f), PdfColumn("Kurz D54 (Kč/USD)", 1f), PdfColumn("Zisk (Kč)", 1f)
        )
        val extraColumns = if (taxableMode) listOf(
            PdfColumn("Prodáno a zdaněno", 1f), PdfColumn("Zdanitelný zisk (Kč)", 1f)
        ) else emptyList()
        val columns = baseColumns + extraColumns

        val rows = report.printableEsppList.map { e ->
            val base = listOf(e.date, fmt(e.amount), e.onePricePurchaseDolarValue, e.onePriceDolarValue, e.buyProfitValue, e.exchange, e.buyCroneProfitValue)
            if (taxableMode) base + listOf(fmt(e.soldAmount), e.taxableBuyCroneProfitValue) else base
        }

        val baseSummary = listOf(
            SummaryCell.empty(),                                // Datum nákupu
            SummaryCell.bold(fmt(report.totalEsppAmount)),      // Počet akcií
            SummaryCell.empty(),                                // Zvýh. nákup. cena (USD)
            SummaryCell.empty(),                                // Tržní cena (USD)
            SummaryCell.bold(fmt(report.profitDolarValue)),     // Zisk (USD)
            SummaryCell.empty(),                                // Kurz D54
            SummaryCell.bold(fmt(report.profitCroneValue))      // Zisk (Kč)
        )
        val extraSummary = if (taxableMode) listOf(
            SummaryCell.empty(),                                          // Prodáno a zdaněno
            SummaryCell.bold(fmt(report.taxableProfitCroneValue))         // Zdanitelný zisk (Kč)
        ) else emptyList()
        val summaryRow = baseSummary + extraSummary

        return PdfReportGenerator.generate(PdfReportDefinition(
            title = "Nepeněžní příjmy dle §6 ze zahraničí – akciový program pro zaměstnance (§6, odst. 3)",
            subtitles = listOf("Program ESPP – nákup akcií za zvýhodněnou cenu", "Cenný papír: Cisco Systems", "Obchodník: $broker"),
            columns = columns, rows = rows, summaryRow = summaryRow,
            landscape = true
        ))
    }
}

