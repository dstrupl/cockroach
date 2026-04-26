package cz.solutions.cockroach

import org.joda.time.LocalDate

data class EsppRecord(
    val date: LocalDate,
    val quantity: Double,
    val purchasePrice: Double,
    val subscriptionFmv: Double,
    val purchaseFmv: Double,
    val purchaseDate: LocalDate,
    val symbol: String = "",
    val broker: String = "",
)