package cz.solutions.cockroach

data class PrintableCzkDividend(
    val symbol: String,
    val broker: String,
    val date: String,
    val brutto: String,
    val tax: String
)
