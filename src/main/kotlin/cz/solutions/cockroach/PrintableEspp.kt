package cz.solutions.cockroach

data class PrintableEspp(
    val date: String,
    val amount: Int,
    val exchange: String,
    val onePricePurchaseDolarValue: String,
    val onePriceDolarValue: String,
    val oneProfitValue: String,
    val buyProfitValue: String,
    val buyCroneProfitValue: String,
    val soldAmount: Double,
    val taxableBuyCroneProfitValue: String
)