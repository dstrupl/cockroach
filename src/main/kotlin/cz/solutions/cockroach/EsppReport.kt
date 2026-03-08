package cz.solutions.cockroach

data class EsppReport(
    val printableEsppList: List<PrintableEspp>,
    val profitCroneValue: Double,
    val profitDolarValue: Double,
    val totalEsppAmount: Double,
    val taxableProfitCroneValue: Double
) {
    fun asMap(): Map<String, Any> {
        return mapOf(
            "esppList" to printableEsppList,
            "profitCroneValue" to FormatingHelper.formatDouble(profitCroneValue),
            "profitDolarValue" to FormatingHelper.formatDouble(profitDolarValue),
            "totalAmount" to totalEsppAmount,
            "taxableProfitCroneValue" to FormatingHelper.formatDouble(taxableProfitCroneValue)
        )
    }
}