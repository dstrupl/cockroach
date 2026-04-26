package cz.solutions.cockroach

import org.joda.time.LocalDate

data class DividendRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency = Currency.USD,
    val symbol: String,
    val broker: String,
)