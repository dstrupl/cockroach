package cz.solutions.cockroach

object SalesReportPdfGenerator {

    fun generate(salesReport: SalesReport): ByteArray {

        val groupHeaders = listOf(
            ColumnGroupHeader("Počet", 1),
            ColumnGroupHeader("Nákup", 6),
            ColumnGroupHeader("Prodej", 6),
            ColumnGroupHeader("Zisk", 2)
        )
        val columns = listOf(
            PdfColumn("# akcií", 50f),

            PdfColumn("Datum", 68f),
            PdfColumn("Cena ($)", 50f),
            PdfColumn("Výdaje ($)", 68f),
            PdfColumn("Kč/USD", 60f),
            PdfColumn("Výdaje (Kč)", 68f),
            PdfColumn("Výdaje (Kč)*", 70f),

            PdfColumn("Datum", 68f),
            PdfColumn("Cena ($)", 50f),
            PdfColumn("Příjmy ($)", 72f),
            PdfColumn("Kč/USD", 60f),
            PdfColumn("Příjmy (Kč)", 72f),
            PdfColumn("Příjmy (Kč)*", 72f),

            PdfColumn("Zisk (Kč)", 60f),
            PdfColumn("Zisk (Kč)*", 68f)
        )

        val rows = salesReport.printableSalesList.map { sale ->
            listOf(
                sale.amount,

                sale.purchaseDate,
                sale.onePurchaseDollar,
                sale.purchaseDollar,
                sale.purchaseExchange,
                sale.purchaseCrone,
                sale.recentPurchaseCrone,

                sale.date,
                sale.oneSellDollar,
                sale.sellDolar,
                sale.sellExchange,
                sale.sellCrone,
                sale.recentSellCrone,

                sale.sellProfitCrone,
                sale.sellRecentProfitCrone
            )
        }

        val fmt = FormatingHelper::formatDouble
        val summaryRow = listOf(
            SummaryCell.regular(fmt(salesReport.totalAmount)),          // # akcií
            SummaryCell.empty(),                                        // Nákup: Datum
            SummaryCell.empty(),                                        // Nákup: Cena ($)
            SummaryCell.regular(fmt(salesReport.buyDollarValue)),       // Nákup: Náklady ($)
            SummaryCell.empty(),                                        // Nákup: Kč/USD
            SummaryCell.regular(fmt(salesReport.buyCroneValue)),        // Nákup: Náklady (Kč)
            SummaryCell.bold(fmt(salesReport.recentBuyCroneValue)),     // Nákup: Náklady (Kč)*
            SummaryCell.empty(),                                        // Prodej: Datum
            SummaryCell.empty(),                                        // Prodej: Cena ($)
            SummaryCell.regular(fmt(salesReport.sellDollarValue)),      // Prodej: Příjem ($)
            SummaryCell.empty(),                                        // Prodej: Kč/USD
            SummaryCell.regular(fmt(salesReport.sellCroneValue)),       // Prodej: Příjem (Kč)
            SummaryCell.bold(fmt(salesReport.recentSellCroneValue)),    // Prodej: Příjem (Kč)*
            SummaryCell.regular(fmt(salesReport.profitCroneValue)),     // Zisk (Kč)
            SummaryCell.bold(fmt(salesReport.recentProfitCroneValue))   // Zisk (Kč)*
        )

        val footerLines = mutableListOf(
            "*) po zohlednění tříletého časového testu."
        )
        if (salesReport.profitForTax == 0.0) {
            footerLines.add("Celková částka prodeje byla menší než 100 000 CZK a je tedy osvobozena od daně.")
        }



        val definition = PdfReportDefinition(
            title = "Ostatní příjmy dle §10 ze zahraničí – zisk z prodeje akcií",
            subtitles = listOf(
                "Cenný papír: Cisco Systems   |   Obchodník: Charles Schwab & Co., Morgan Stanley & Co."
            ),
            columnGroupHeaders = groupHeaders,
            columns = columns,
            rows = rows,
            summaryRow = summaryRow,
            footerLines = footerLines
        )

        return PdfReportGenerator.generate(definition)
    }
}
