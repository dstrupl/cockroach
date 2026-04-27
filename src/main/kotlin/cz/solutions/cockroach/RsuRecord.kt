package cz.solutions.cockroach

import org.joda.time.LocalDate

data class RsuRecord(
    val date: LocalDate,
    val quantity: Int,
    val vestFmv: Double,
    val vestDate: LocalDate,
    val grantId: String,
    val symbol: String,
    val broker: String,
)