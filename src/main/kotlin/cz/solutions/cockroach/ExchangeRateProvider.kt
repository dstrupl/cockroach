package cz.solutions.cockroach

import org.joda.time.LocalDate

fun interface ExchangeRateProvider {
    fun rateAt(day: LocalDate): Double
}