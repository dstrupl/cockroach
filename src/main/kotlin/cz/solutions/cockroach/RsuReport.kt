package cz.solutions.cockroach

data class RsuReport(
    val printableRsuList: List<PrintableRsu>,
    val rsuCroneValue: Double,
    val rsuDollarValue: Double,
    val taxableRsuCroneValue: Double,
    val totalAmount: Int
) {
    fun asMap(): Map<String, Any> {
        return mapOf(
            "rsuList" to printableRsuList,
            "rsuCroneValue" to FormatingHelper.formatDouble(rsuCroneValue),
            "rsuDolarValue" to FormatingHelper.formatDouble(rsuDollarValue),
            "totalAmount" to totalAmount,
            "taxableRsuCroneValue" to FormatingHelper.formatDouble(taxableRsuCroneValue)
        )
    }
}