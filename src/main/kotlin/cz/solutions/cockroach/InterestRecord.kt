package cz.solutions.cockroach

import org.joda.time.LocalDate

data class InterestRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency,
    val product: String,
    val broker: String,
    /** Withholding tax already deducted at source, stored as a non-negative value in the source currency. */
    val tax: Double,
    /** ISO 3166-1 alpha-2 country code identifying the source of the interest income (e.g. "IE", "SK", "CZ"). */
    val country: String,
)
