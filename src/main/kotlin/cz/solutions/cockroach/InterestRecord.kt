package cz.solutions.cockroach

import org.joda.time.LocalDate

data class InterestRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency = Currency.USD
)
