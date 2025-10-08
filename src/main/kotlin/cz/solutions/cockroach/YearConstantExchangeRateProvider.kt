package cz.solutions.cockroach

import org.joda.time.LocalDate

class YearConstantExchangeRateProvider(private val exchange: Map<Int, Double>) : ExchangeRateProvider {

    companion object {
        fun hardcoded(): YearConstantExchangeRateProvider {
            return YearConstantExchangeRateProvider(
                mapOf(
                    2018 to 21.780,
                    2019 to 22.930,
                    2020 to 23.140,
                    2021 to 21.72,
                    2022 to 23.41,
                    2023 to 22.14,
                    2024 to 23.28
                )
            )
        }
    }

    override fun rateAt(day: LocalDate): Double {
        return exchange[day.year] ?: throw IllegalArgumentException("can not find rate for $day")
    }
}