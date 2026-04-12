package cz.solutions.cockroach

data class SalesReport(
    val printableSalesList: List<PrintableSale>,
    val sellCroneValue: Double,
    val recentSellCroneValue: Double,
    val sellDollarValue: Double,
    val profitCroneValue: Double,
    val recentProfitCroneValue: Double,
    val totalAmount: Double,
    val buyDollarValue: Double,
    val profitForTax: Double,
    val buyCroneValue: Double,
    val recentBuyCroneValue: Double,
    val sellCroneForTax: Double,
    val buyCroneForTax: Double
)