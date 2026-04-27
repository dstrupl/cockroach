package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object EsppReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateEsppReport(
        esppRecordList: List<EsppRecord>,
        saleRecordList: List<SaleRecord>,
        interval: DateInterval,
        exchangeRateProvider: ExchangeRateProvider,
        taxableAmount: (Double, Double) -> Double = { quantity,_ -> quantity }
    ): EsppReport {
        val esppRecords = esppRecordList
            .filter { interval.contains(it.purchaseDate) }
            .sortedBy { it.date }

        val esppSaleRecordsByDate = saleRecordList
            .filter { interval.contains(it.date) && it.type == "ESPP" }
            .groupBy { it.purchaseDate }

        val esppInfos = esppRecords.map { espp ->
            val soldQuantity = esppSaleRecordsByDate[espp.purchaseDate]
                ?.sumOf { it.quantity } ?: 0.0
            withConvertedPrices(espp, soldQuantity,taxableAmount(espp.quantity,soldQuantity), exchangeRateProvider)
        }

        return EsppReport(
            printableEsppList = esppInfos.map { it.toPrintable() },
            profitCroneValue = esppInfos.sumOf { it.buyCroneProfitValue },
            profitDolarValue = esppInfos.sumOf { it.buyProfitValue },
            totalEsppAmount = esppInfos.sumOf { it.amount },
            taxableProfitCroneValue = esppInfos.sumOf { it.taxableBuyCroneProfitValue }
        )
    }

    private fun withConvertedPrices(espp: EsppRecord, soldAmount: Double, taxableAmount: Double, exchangeRateProvider: ExchangeRateProvider): EsppInfo {
        val exchange = exchangeRateProvider.rateAt(espp.purchaseDate, Currency.USD)
        val partialProfit = espp.purchaseFmv - espp.purchasePrice

        return EsppInfo(
            symbol = espp.symbol,
            broker = espp.broker,
            date = espp.purchaseDate,
            amount = espp.quantity,
            exchange = exchange,
            onePricePurchaseDolarValue = espp.purchasePrice,
            onePriceDolarValue = espp.purchaseFmv,
            oneProfitValue = partialProfit,
            buyProfitValue = partialProfit * espp.quantity,
            buyCroneProfitValue = partialProfit * espp.quantity * exchange,
            soldAmount = soldAmount,
            taxableBuyCroneProfitValue = partialProfit * taxableAmount * exchange
        )
    }

    private data class EsppInfo(
        val symbol: String,
        val broker: String,
        val date: LocalDate,
        val amount: Double,
        val exchange: Double,
        val onePricePurchaseDolarValue: Double,
        val onePriceDolarValue: Double,
        val oneProfitValue: Double,
        val buyProfitValue: Double,
        val buyCroneProfitValue: Double,
        val soldAmount: Double,
        val taxableBuyCroneProfitValue: Double
    ) {
        fun toPrintable(): PrintableEspp {
            return PrintableEspp(
                symbol = symbol,
                broker = broker,
                date = DATE_FORMATTER.print(date),
                amount = amount,
                exchange = FormatingHelper.formatExchangeRate(exchange),
                onePricePurchaseDolarValue = FormatingHelper.formatDouble(onePricePurchaseDolarValue),
                onePriceDolarValue = FormatingHelper.formatDouble(onePriceDolarValue),
                oneProfitValue = FormatingHelper.formatDouble(oneProfitValue),
                buyProfitValue = FormatingHelper.formatDouble(buyProfitValue),
                buyCroneProfitValue = FormatingHelper.formatDouble(buyCroneProfitValue),
                soldAmount = soldAmount,
                taxableBuyCroneProfitValue = FormatingHelper.formatDouble(taxableBuyCroneProfitValue)
            )
        }
    }
}