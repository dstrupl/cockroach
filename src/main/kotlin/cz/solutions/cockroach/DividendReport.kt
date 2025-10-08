package cz.solutions.cockroach

data class DividendReport(
    val printableDividendList: List<PrintableDividend>,
    val totalBruttoDollar: Double,
    val totalTaxDollar: Double,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
    val totalTaxReversalDollar: Double,
    val totalTaxReversalCrown: Double
) {
    fun asMap(): Map<String, Any> {
        return mapOf(
            "dividendList" to printableDividendList,
            "totalBruttoDollar" to FormatingHelper.formatDouble(totalBruttoDollar),
            "totalTaxDollar" to FormatingHelper.formatDouble(totalTaxDollar),
            "totalBruttoCrown" to FormatingHelper.formatDouble(totalBruttoCrown),
            "totalTaxCrown" to FormatingHelper.formatDouble(totalTaxCrown),
            "totalTaxReversal" to if (totalTaxReversalDollar > 0) FormatingHelper.formatDouble(totalTaxReversalDollar) else "",
            "totalTaxReversalCrown" to if (totalTaxReversalCrown > 0) FormatingHelper.formatDouble(totalTaxReversalCrown) else ""
        )
    }
}