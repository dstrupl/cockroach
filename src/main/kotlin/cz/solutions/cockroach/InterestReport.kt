package cz.solutions.cockroach

data class PrintableInterest(
    val product: String,
    val broker: String,
    val date: String,
    val brutto: String,
    val exchange: String,
    val bruttoCrown: String
)

data class PrintableCzkInterest(
    val product: String,
    val broker: String,
    val date: String,
    val brutto: String
)

data class CurrencyInterestSection(
    val currency: Currency,
    val printableInterestList: List<PrintableInterest>,
    val totalBrutto: Double,
    val totalBruttoCrown: Double
)

data class CzkInterestSection(
    val printableInterestList: List<PrintableCzkInterest>,
    val totalBruttoCrown: Double
)

data class InterestReport(
    val sections: List<CurrencyInterestSection>,
    val czkSection: CzkInterestSection?
) {
    val totalNonCzkBruttoCrown: Double get() = sections.sumOf { it.totalBruttoCrown }
    val totalCzkBruttoCrown: Double get() = czkSection?.totalBruttoCrown ?: 0.0
    val totalBruttoCrown: Double get() = totalNonCzkBruttoCrown + totalCzkBruttoCrown
}
