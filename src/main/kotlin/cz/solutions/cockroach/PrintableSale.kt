package cz.solutions.cockroach

data class PrintableSale(
    val symbol: String,
    val broker: String,

    val amount: String,


    val purchaseDate: String,
    val onePurchaseDollar: String,
    val purchaseDollar: String,
    val purchaseExchange: String,
    val purchaseCrone: String,
    val recentPurchaseCrone: String,


    val date: String,
    val oneSellDollar: String,
    val sellDolar: String,
    val sellExchange: String,
    val sellCrone: String,
    val recentSellCrone: String,

    val sellProfitCrone: String,
    val sellRecentProfitCrone: String




)