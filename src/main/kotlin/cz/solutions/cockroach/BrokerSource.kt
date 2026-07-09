package cz.solutions.cockroach

/**
 * Single per-broker entry point used by [CockroachMain.report].
 *
 * Each implementation owns the entire mapping from "the configured input(s) for this broker" to a
 * [ParsedExport]. To add a new broker, drop in a new [BrokerSource] implementation and instantiate
 * it from [runCockroach]; nothing else in [CockroachMain] needs to change.
 */
interface BrokerSource {
    /** Human-readable name used in error messages and recommendations (e.g. "Schwab", "VÚB"). */
    val name: String

    /** Parses this broker's configured inputs into a [ParsedExport]. */
    fun parse(): ParsedExport
}
