package cz.solutions.cockroach

data class PrintableEspp(
    val symbol: String,
    val broker: String,
    val date: String,
    val amount: Double,
    val exchange: String,
    val onePricePurchaseDolarValue: String,
    val onePriceDolarValue: String,
    val oneProfitValue: String,
    val buyProfitValue: String,
    val buyCroneProfitValue: String,
    val soldAmount: Double,
    val taxableBuyCroneProfitValue: String
)