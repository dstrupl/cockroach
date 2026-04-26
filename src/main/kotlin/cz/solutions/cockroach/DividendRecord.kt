package cz.solutions.cockroach

import org.joda.time.LocalDate

data class DividendRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency,
    val symbol: String,
    val broker: String,
    /** ISO 3166-1 alpha-2 country of issuer, derived from the first two letters of the ISIN.
     *  "CZ" routes the dividend to the Czech-source (final withholding) section; everything else
     *  is reported as foreign income on Příloha č. 3. */
    val country: String,
)