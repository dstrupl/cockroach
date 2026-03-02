package cz.solutions.cockroach

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

object ExchangeRatesReader {

    fun parse(vararg files: String): TabularExchangeRateProvider {
        val mapping = files.map { parseOne(it) }
            .reduce { acc, map -> acc + map }
        return TabularExchangeRateProvider(mapping)
    }

    private fun parseOne(data: String): Map<LocalDate, Double> {
        val lines = data.lines()
        val header = lines.first()
        val headerParts = header.split("|")
        val usdIndex = headerParts.indexOf("1 USD")
        return lines
            .drop(1)
            .filter { it.isNotBlank() }
            .map { parseLine(it, usdIndex) }
            .associate { it.date to it.getAmount() }
    }

    private fun parseLine(line: String, usdIndex: Int): Line {
        val formatter = DateTimeFormat.forPattern("dd.MM.YYYY")
        val parts = line.split("|")
        return Line(LocalDate.parse(parts[0], formatter), Money.fromString(parts[usdIndex]))
    }

    private data class Line(val date: LocalDate, val rate: Money) {
        fun getAmount(): Double {
            return rate.amount
        }
    }

    private data class Money(val amount: Double) {
        companion object {
            fun fromString(input: String): Money {
                return Money(input.replace(',', '.').toDouble())
            }
        }
    }
}