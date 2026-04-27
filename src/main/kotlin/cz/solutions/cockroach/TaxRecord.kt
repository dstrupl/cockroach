package cz.solutions.cockroach

import org.joda.time.LocalDate

data class TaxRecord(
    val date: LocalDate,
    val amount: Double,
    val currency: Currency,
    /** Issuer symbol of the underlying dividend, used to pair the tax row with its DividendRecord
     *  in [DividentReportPreparation]. Must match the dividend's symbol exactly. */
    val symbol: String,
    /** Broker that produced both the dividend and this tax row; part of the pairing key so two
     *  brokers paying the same symbol on the same date cannot cross-match. */
    val broker: String,
)