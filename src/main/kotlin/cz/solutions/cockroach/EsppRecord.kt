package cz.solutions.cockroach

import org.joda.time.LocalDate

data class EsppRecord(
    val date: LocalDate,
    val quantity: Int,
    val purchasePrice: Double,
    val subscriptionFmv: Double,
    val purchaseFmv: Double,
    val purchaseDate: LocalDate
)