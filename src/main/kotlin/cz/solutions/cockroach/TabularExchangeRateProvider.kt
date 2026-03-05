package cz.solutions.cockroach

import org.joda.time.LocalDate
import java.nio.charset.StandardCharsets
import java.util.*

class TabularExchangeRateProvider(knownRates: Map<LocalDate, Double>) : ExchangeRateProvider {
    private val knownRates: NavigableMap<LocalDate, Double> = TreeMap(knownRates)

    companion object {
        fun hardcoded(): TabularExchangeRateProvider {
            return ExchangeRatesReader.parse(
                load("rates_2021.txt"),
                load("rates_2022_a.txt"),
                load("rates_2022_b.txt"),
                load("rates_2023.txt"),
                load("rates_2024.txt"),
                load("rates_2025.txt")
            )
        }

        private fun load(fileName: String): String {
            return TabularExchangeRateProvider::class.java.getResourceAsStream(fileName)?.use {
                it.reader(StandardCharsets.UTF_8).readText()
            } ?: throw RuntimeException("Could not load template $fileName")
        }
    }

    override fun rateAt(day: LocalDate): Double {
        return knownRates.floorEntry(day)?.value
            ?: throw IllegalArgumentException("can not find rate for $day")
    }
}