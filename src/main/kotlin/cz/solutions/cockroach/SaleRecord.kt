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
    val grantId: String?
) {
    fun isTaxable(): Boolean {
        return date.isBefore(purchaseDate.plusYears(3))
    }
}