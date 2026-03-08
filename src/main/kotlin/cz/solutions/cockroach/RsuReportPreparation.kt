package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

object RsuReportPreparation {
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC()

    fun generateRsuReport(
        rsuRecordList: List<RsuRecord>,
        saleRecordList: List<SaleRecord>,
        interval: DateInterval,
        exchangeRateProvider: ExchangeRateProvider,
        taxableAmount: (Double, Double) -> Double = { quantity,_ -> quantity }
    ): RsuReport {
        val rsuRecords = rsuRecordList
            .filter { interval.contains(it.vestDate) }
            .sortedBy { it.date }

        val rsuSaleRecordsByDateAndGrantId = saleRecordList
            .filter { interval.contains(it.date) && it.type == "RS" }
            .groupBy { it.purchaseDate to it.grantId }

        val rsuInfos = rsuRecords.map { rsu ->
            val soldQuantity = rsuSaleRecordsByDateAndGrantId[rsu.vestDate to rsu.grantId]
                ?.sumOf { it.quantity } ?: 0.0
            withConvertedPrices(rsu, soldQuantity,taxableAmount(rsu.quantity.toDouble(),soldQuantity), exchangeRateProvider)
        }

        return RsuReport(
            printableRsuList = rsuInfos.map { it.toPrintable() },
            rsuCroneValue = rsuInfos.sumOf { it.vestCroneValue },
            rsuDollarValue = rsuInfos.sumOf { it.vestDolarValue },
            taxableRsuCroneValue = rsuInfos.sumOf { it.taxableVestCroneValue },
            totalAmount = rsuInfos.sumOf { it.amount }
        )
    }

    private fun withConvertedPrices(rsu: RsuRecord, soldAmount: Double, taxableAmount: Double,exchangeRateProvider: ExchangeRateProvider): RsuInfo {
        val exchange = exchangeRateProvider.rateAt(rsu.vestDate)
        val partialRsuDolarValue = rsu.quantity * rsu.vestFmv
        val partialRsuCroneValue = partialRsuDolarValue * exchange
        val taxableVestCroneValue = taxableAmount * rsu.vestFmv * exchange

        return RsuInfo(
            date = rsu.vestDate,
            amount = rsu.quantity,
            exchange = exchange,
            onePriceDolarValue = rsu.vestFmv,
            vestDolarValue = partialRsuDolarValue,
            vestCroneValue = partialRsuCroneValue,
            soldAmount = soldAmount,
            taxableVestCroneValue = taxableVestCroneValue
        )
    }

    private data class RsuInfo(
        val date: LocalDate,
        val amount: Int,
        val exchange: Double,
        val onePriceDolarValue: Double,
        val vestDolarValue: Double,
        val vestCroneValue: Double,
        val soldAmount: Double,
        val taxableVestCroneValue: Double
    ) {
        fun toPrintable(): PrintableRsu {
            return PrintableRsu(
                date = DATE_FORMATTER.print(date),
                amount = amount,
                exchange = FormatingHelper.formatExchangeRate(exchange),
                onePriceDolarValue = FormatingHelper.formatDouble(onePriceDolarValue),
                vestDolarValue = FormatingHelper.formatDouble(vestDolarValue),
                vestCroneValue = FormatingHelper.formatDouble(vestCroneValue),
                soldAmount = FormatingHelper.formatDouble(soldAmount),
                taxableVestCroneValue = FormatingHelper.formatDouble(taxableVestCroneValue)
            )
        }
    }
}