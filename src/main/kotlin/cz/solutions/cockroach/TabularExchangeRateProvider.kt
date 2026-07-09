package cz.solutions.cockroach

import org.joda.time.LocalDate
import java.util.*

class TabularExchangeRateProvider(
    knownRates: Map<LocalDate, Map<Currency, Double>>
) : ExchangeRateProvider {
    private val knownRates: NavigableMap<LocalDate, Map<Currency, Double>> = TreeMap(knownRates)

    companion object {
        fun hardcoded(): TabularExchangeRateProvider {
            return fromSource(ClasspathCnbYearRatesSource(), 2021..2025)
        }

        fun fromSource(source: CnbYearRatesSource, years: IntRange): TabularExchangeRateProvider {
            val chunks = years.flatMap { source.loadYear(it) }
            return ExchangeRatesReader.parse(*chunks.toTypedArray())
        }
    }

    override fun rateAt(day: LocalDate, currency: Currency): Double {
        if (currency == Currency.CZK) return 1.0
        val perCurrency = knownRates.floorEntry(day)?.value
            ?: throw IllegalArgumentException("can not find rate for $day")
        return perCurrency[currency]
            ?: throw IllegalArgumentException("can not find rate for $day in $currency")
    }
}