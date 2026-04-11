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
        var profitCroneValue = 0.0
        var recentProfitCroneValue = 0.0
        var recentSellCroneValue = 0.0
        var recentBuyCroneValue = 0.0
        var totalAmount = 0.0

        val printableSalesList = filteredSaleRecords.map { sale ->
            val exchange = exchangeRateProvider.rateAt(sale.date)

            val partialSellDolarValue = sale.quantity * sale.salePrice
            val partialSellCroneValue = partialSellDolarValue * exchange

            val buyPriceDolarValue = sale.quantity * sale.purchaseFmv
            val buyPriceCroneValue = buyPriceDolarValue * exchange //todo

            val partialProfitCroneValue = partialSellCroneValue - buyPriceCroneValue
            val partialRecentProfitCroneValue = if (sale.isTaxable()) partialProfitCroneValue else 0.0


            sellDollarValue += partialSellDolarValue
            sellCroneValue += partialSellCroneValue
            profitCroneValue += partialProfitCroneValue
            recentProfitCroneValue += partialRecentProfitCroneValue

            if (sale.isTaxable()) {
                recentSellCroneValue += partialSellCroneValue
                recentBuyCroneValue += buyPriceDolarValue * exchange
            }
            totalAmount += sale.quantity

            PrintableSale(
                amount = FormatingHelper.formatDouble(sale.quantity),

                purchaseDate = DATE_FORMATTER.print(sale.purchaseDate),
                onePurchaseDollar = FormatingHelper.formatDouble(sale.purchaseFmv),
                purchaseDollar = FormatingHelper.formatDouble(buyPriceDolarValue),
                purchaseExchange = FormatingHelper.formatExchangeRate(exchange), //todo
                purchaseCrone = FormatingHelper.formatDouble(buyPriceCroneValue),

                date = DATE_FORMATTER.print(sale.date),
                oneSellDollar = FormatingHelper.formatDouble(sale.salePrice),
                sellDolar = FormatingHelper.formatDouble(partialSellDolarValue),
                sellExchange = FormatingHelper.formatExchangeRate(exchange),
                sellCrone = FormatingHelper.formatDouble(partialSellCroneValue),


                sellProfitCrone = FormatingHelper.formatDouble(partialRecentProfitCroneValue),
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
            profitCroneValue = profitCroneValue,
            recentProfitCroneValue = recentProfitCroneValue,
            totalAmount = totalAmount,
            profitForTax = profitForTax,
            sellCroneForTax = sellCroneForTax,
            buyCroneForTax = buyCroneForTax
        )
    }
}