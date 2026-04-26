package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object ExchangeRatesReader {

    private val CURRENCY_HEADERS = mapOf(
        Currency.USD to "1 USD",
        Currency.EUR to "1 EUR",
        Currency.GBP to "1 GBP"
    )

    fun parse(vararg files: String): TabularExchangeRateProvider {
        val mapping = files.map { parseOne(it) }
            .reduce { acc, map -> acc + map }
        return TabularExchangeRateProvider(mapping)
    }

    private fun parseOne(data: String): Map<LocalDate, Map<Currency, Double>> {
        val lines = data.lines()
        val header = lines.first()
        val headerParts = header.split("|")
        val indices = CURRENCY_HEADERS.mapValues { (currency, columnHeader) ->
            val idx = headerParts.indexOf(columnHeader)
            require(idx >= 0) { "missing $columnHeader column for $currency in header: $header" }
            idx
        }
        return lines
            .drop(1)
            .filter { it.isNotBlank() }
            .associate { parseLine(it, indices) }
    }

    private fun parseLine(line: String, indices: Map<Currency, Int>): Pair<LocalDate, Map<Currency, Double>> {
        val formatter = DateTimeFormat.forPattern("dd.MM.YYYY")
        val parts = line.split("|")
        val date = LocalDate.parse(parts[0], formatter)
        val rates = indices.mapValues { (_, idx) -> Money.fromString(parts[idx]).amount }
        return date to rates
    }

    private data class Money(val amount: Double) {
        companion object {
            fun fromString(input: String): Money {
                return Money(input.replace(',', '.').toDouble())
            }
        }
    }
}