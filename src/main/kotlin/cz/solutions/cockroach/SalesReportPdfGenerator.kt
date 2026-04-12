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
            PdfColumn("Náklady ($)", 68f),
            PdfColumn("Kč/USD", 60f),
            PdfColumn("Náklady (Kč)", 68f),
            PdfColumn("Náklady (Kč)*", 70f),

            PdfColumn("Datum", 68f),
            PdfColumn("Cena ($)", 50f),
            PdfColumn("Příjem ($)", 72f),
            PdfColumn("Kč/USD", 60f),
            PdfColumn("Příjem (Kč)", 72f),
            PdfColumn("Příjem (Kč)*", 72f),

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

        val summaryRow = listOf(
            FormatingHelper.formatDouble(salesReport.totalAmount), // # akcií
            "", // Nákup: Datum
            "", // Nákup: Cena ($)
            FormatingHelper.formatDouble(salesReport.buyDollarValue), // Nákup: Náklady ($)
            "", // Nákup: Kč/USD
            FormatingHelper.formatDouble(salesReport.buyCroneValue), // Nákup: Náklady (Kč)
            FormatingHelper.formatDouble(salesReport.recentBuyCroneValue), // Nákup: Náklady (Kč)*
            "", // Prodej: Datum
           "", // Prodej: Cena ($)
            FormatingHelper.formatDouble(salesReport.sellDollarValue), // Prodej: Příjem ($)
            "", // Prodej: Kč/USD
            FormatingHelper.formatDouble(salesReport.sellCroneValue), // Prodej: Příjem (Kč)
            FormatingHelper.formatDouble(salesReport.recentSellCroneValue), // Prodej: Příjem (Kč)*
            FormatingHelper.formatDouble(salesReport.profitCroneValue), // Zisk (Kč)
            FormatingHelper.formatDouble(salesReport.recentProfitCroneValue) // Zisk (Kč)*
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
