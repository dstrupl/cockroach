package cz.solutions.cockroach

import org.joda.time.LocalDate

data class TaxRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency = Currency.USD
)