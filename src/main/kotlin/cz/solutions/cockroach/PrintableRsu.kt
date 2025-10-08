package cz.solutions.cockroach

data class PrintableRsu(
    val date: String,
    val amount: Int,
    val exchange: String,
    val onePriceDolarValue: String,
    val vestDolarValue: String,
    val vestCroneValue: String,
    val soldAmount: String,
    val taxableVestCroneValue: String
)