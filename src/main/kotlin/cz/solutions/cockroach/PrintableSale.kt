package cz.solutions.cockroach

data class PrintableSale(
    val date: String,
    val purchaseDate: String,
    val amount: String,
    val exchange: String,
    val onePurchaseDollar: String,
    val oneSellDollar: String,
    val oneProfitDollar: String,
    val sellDolar: String,
    val sellProfitDollar: String,
    val sellCrone: String,
    val sellRecentProfitCrone: String
)