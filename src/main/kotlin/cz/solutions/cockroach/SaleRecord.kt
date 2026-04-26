package cz.solutions.cockroach

import org.joda.time.LocalDate

data class SaleRecord(
    val date: LocalDate,
    val type: String,
    val quantity: Double,
    val salePrice: Double,
    val purchasePrice: Double,
    val purchaseFmv: Double,
    val purchaseDate: LocalDate,
    val grantId: String?,
    val symbol: String,
    val broker: String,
) {
    fun isTaxable(): Boolean {
        return date.isBefore(purchaseDate.plusYears(3))
    }
}