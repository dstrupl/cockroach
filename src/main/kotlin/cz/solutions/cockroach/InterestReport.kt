package cz.solutions.cockroach

data class PrintableInterest(
    val product: String,
    val broker: String,
    val date: String,
    val brutto: String,
    val tax: String,
    val exchange: String,
    val bruttoCrown: String,
    val taxCrown: String,
)

data class PrintableCzkInterest(
    val product: String,
    val broker: String,
    val date: String,
    val brutto: String,
    val tax: String,
)

data class CurrencyInterestSection(
    val country: String,
    val currency: Currency,
    val printableInterestList: List<PrintableInterest>,
    val totalBrutto: Double,
    val totalTax: Double,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
)

data class CzkInterestSection(
    val country: String,
    val printableInterestList: List<PrintableCzkInterest>,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
)

/** Aggregate of all interest income (CZK and non-CZK) per source country, in CZK. */
data class CountryInterestTotal(
    val country: String,
    val totalBruttoCrown: Double,
    val totalTaxCrown: Double,
) {
    val totalBruttoCrownFormatted: String get() = FormatingHelper.formatRounded(totalBruttoCrown)
    val totalTaxCrownFormatted: String get() = FormatingHelper.formatRounded(totalTaxCrown)
}

data class InterestReport(
    val sections: List<CurrencyInterestSection>,
    val czkSections: List<CzkInterestSection>,
    val countryTotals: List<CountryInterestTotal>,
) {
    val totalNonCzkBruttoCrown: Double get() = sections.sumOf { it.totalBruttoCrown }
    val totalCzkBruttoCrown: Double get() = czkSections.sumOf { it.totalBruttoCrown }
    val totalBruttoCrown: Double get() = totalNonCzkBruttoCrown + totalCzkBruttoCrown

    /** Single CZK domestic section ("CZ" country), if any – preserved for legacy assertions. */
    val czkSection: CzkInterestSection? get() = czkSections.firstOrNull { it.country == "CZ" }

    /** Country totals limited to foreign sources (i.e. anything that should land on Příloha č. 3). */
    val foreignCountryTotals: List<CountryInterestTotal> get() = countryTotals.filter { it.country != "CZ" }
}
