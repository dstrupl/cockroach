package cz.solutions.cockroach

data class PrintableDividend(
    val symbol: String,
    val broker: String,
    val date: String,
    val brutto: String,
    val exchange: String,
    val tax: String,
    val bruttoCrown: String,
    val taxCrown: String
)