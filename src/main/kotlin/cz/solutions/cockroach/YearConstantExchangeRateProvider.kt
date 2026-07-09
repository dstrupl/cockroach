package cz.solutions.cockroach

import org.joda.time.LocalDate

class YearConstantExchangeRateProvider(
    private val exchange: Map<Int, Map<Currency, Double>>
) : ExchangeRateProvider {

    companion object {
        fun usdOnly(map: Map<Int, Double>): YearConstantExchangeRateProvider {
            return YearConstantExchangeRateProvider(
                map.mapValues { (_, rate) -> mapOf(Currency.USD to rate) }
            )
        }

        fun hardcoded(): YearConstantExchangeRateProvider {
            return YearConstantExchangeRateProvider(
                mapOf(
                    2018 to mapOf(Currency.USD to 21.780),
                    2019 to mapOf(Currency.USD to 22.930),
                    2020 to mapOf(Currency.USD to 23.140),
                    2021 to mapOf(
                        Currency.USD to 21.72,
                        Currency.EUR to 25.65,
                        Currency.GBP to 29.88
                    ),
                    2022 to mapOf(
                        Currency.USD to 23.41,
                        Currency.EUR to 24.54,
                        Currency.GBP to 28.72
                    ),
                    2023 to mapOf(
                        Currency.USD to 22.14,
                        Currency.EUR to 23.97,
                        Currency.GBP to 27.59
                    ),
                    2024 to mapOf(
                        Currency.USD to 23.28,
                        Currency.EUR to 25.16,
                        Currency.GBP to 29.78
                    ),
                    2025 to mapOf(
                        Currency.USD to 21.84,
                        Currency.EUR to 24.66,
                        Currency.GBP to 28.80
                    )
                )
            )
        }
    }

    override fun rateAt(day: LocalDate, currency: Currency): Double {
        if (currency == Currency.CZK) return 1.0
        val perCurrency = exchange[day.year]
            ?: throw IllegalArgumentException("can not find rate for $day")
        return perCurrency[currency]
            ?: throw IllegalArgumentException("can not find rate for $day in $currency")
    }
}