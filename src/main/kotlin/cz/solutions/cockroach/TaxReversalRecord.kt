package cz.solutions.cockroach

import org.joda.time.LocalDate

data class TaxReversalRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency,
)