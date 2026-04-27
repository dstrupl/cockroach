package cz.solutions.cockroach

data class CurrencyDividendSection(
    val currency: Currency,
    val printableDividendList: List<PrintableDividend>,
    val totalBrutto: Double,
    val totalTax: Double,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
    val totalTaxReversal: Double,
    val totalTaxReversalCrown: Double
)

data class CzkDividendSection(
    val printableDividendList: List<PrintableCzkDividend>,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
    val totalTaxReversalCrown: Double
)

data class DividendReport(
    val sections: List<CurrencyDividendSection>,
    val czkSection: CzkDividendSection?
) {
    val totalNonCzkBruttoCrown: Double get() = sections.sumOf { it.totalBruttoCrown }
    val totalNonCzkTaxCrown: Double get() = sections.sumOf { it.totalTaxCrown }
}