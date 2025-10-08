package cz.solutions.cockroach

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object SalesReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateSalesReport(salesRecords: List<SaleRecord>, interval: DateInterval, exchangeRateProvider: ExchangeRateProvider): SalesReport {

        val filteredSaleRecords = salesRecords
            .filter { interval.contains(it.date) }
            .sortedBy { it.date }

        var sellDollarValue = 0.0
        var sellCroneValue = 0.0
        var profitDolarValue = 0.0
        var recentProfitCroneValue = 0.0
        var recentSellCroneValue = 0.0
        var recentBuyCroneValue = 0.0
        var totalAmount = 0.0

        val printableSalesList = filteredSaleRecords.map { sale ->
            val exchange = exchangeRateProvider.rateAt(sale.date)

            val partialSellDolarValue = sale.quantity * sale.salePrice
            val partialSellCroneValue = partialSellDolarValue * exchange
            val buyPriceDolarValue = sale.quantity * sale.purchaseFmv

            val partialProfitValue = partialSellDolarValue - buyPriceDolarValue
            val partialRecentProfitValue = if (sale.isTaxable()) partialProfitValue else 0.0
            val partialRecentProfitCroneValue = partialRecentProfitValue * exchange

            sellDollarValue += partialSellDolarValue
            sellCroneValue += partialSellCroneValue
            profitDolarValue += partialProfitValue
            recentProfitCroneValue += partialRecentProfitCroneValue

            if (sale.isTaxable()) {
                recentSellCroneValue += partialSellCroneValue
                recentBuyCroneValue += buyPriceDolarValue * exchange
            }
            totalAmount += sale.quantity

            PrintableSale(
                date = DATE_FORMATTER.print(sale.date),
                purchaseDate = DATE_FORMATTER.print(sale.purchaseDate),
                amount = FormatingHelper.formatDouble(sale.quantity),
                exchange = FormatingHelper.formatExchangeRate(exchange),
                onePurchaseDollar = FormatingHelper.formatDouble(sale.purchaseFmv),
                oneSellDollar = FormatingHelper.formatDouble(sale.salePrice),
                oneProfitDollar = FormatingHelper.formatDouble(sale.salePrice - sale.purchaseFmv),
                sellDolar = FormatingHelper.formatDouble(partialSellDolarValue),
                sellProfitDollar = FormatingHelper.formatDouble(partialProfitValue),
                sellCrone = FormatingHelper.formatDouble(partialSellCroneValue),
                sellRecentProfitCrone = FormatingHelper.formatDouble(partialRecentProfitCroneValue)
            )
        }

        val (profitForTax, sellCroneForTax, buyCroneForTax) = if (sellCroneValue < 100000) {
            Triple(0.0, 0.0, 0.0)
        } else {
            Triple(recentProfitCroneValue, recentSellCroneValue, recentBuyCroneValue)
        }

        return SalesReport(
            printableSalesList = printableSalesList,
            sellCroneValue = sellCroneValue,
            sellDollarValue = sellDollarValue,
            profitDolarValue = profitDolarValue,
            recentProfitCroneValue = recentProfitCroneValue,
            totalAmount = totalAmount,
            profitForTax = profitForTax,
            sellCroneForTax = sellCroneForTax,
            buyCroneForTax = buyCroneForTax
        )
    }
}