package cz.solutions.cockroach

import org.joda.time.LocalDate

data class TaxReversalRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency,
    /** Issuer symbol the original withholding belonged to. Carried for reporting parity with
     *  [TaxRecord]; reversals are aggregated per currency rather than paired to a single dividend. */
    val symbol: String,
    /** Broker that produced the original withholding and this reversal. */
    val broker: String,
)