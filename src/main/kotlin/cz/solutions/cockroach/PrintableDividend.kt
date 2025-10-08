package cz.solutions.cockroach

data class PrintableDividend(
    val date: String,
    val bruttoDollar: String,
    val exchange: String,
    val taxDollar: String,
    val bruttoCrown: String,
    val taxCrown: String
)