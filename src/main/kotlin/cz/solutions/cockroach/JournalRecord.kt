package cz.solutions.cockroach

import org.joda.time.LocalDate

data class JournalRecord(
    val date: LocalDate,
    val amount: Double,
    val description: String
)