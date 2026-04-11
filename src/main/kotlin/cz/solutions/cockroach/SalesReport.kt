package cz.solutions.cockroach

data class SalesReport(
    val printableSalesList: List<PrintableSale>,
    val sellCroneValue: Double,
    val sellDollarValue: Double,
    val profitCroneValue: Double,
    val recentProfitCroneValue: Double,
    val totalAmount: Double,
    val profitForTax: Double,
    val sellCroneForTax: Double,
    val buyCroneForTax: Double
) {
    fun asMap(): Map<String, Any> {
        return mapOf(
            "salesList" to printableSalesList,
            "sellCroneValue" to FormatingHelper.formatDouble(sellCroneValue),
            "sellDollarValue" to FormatingHelper.formatDouble(sellDollarValue),
            "profitCroneValue" to FormatingHelper.formatDouble(profitCroneValue),
            "profitRecentCroneValue" to FormatingHelper.formatDouble(recentProfitCroneValue),
            "totalAmount" to totalAmount,
            "profitForTax" to if (profitForTax > 0) FormatingHelper.formatDouble(profitForTax) else ""
        )
    }
}