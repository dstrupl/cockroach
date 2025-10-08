package cz.solutions.cockroach

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

internal class TabularExchangeRateProviderTest {

    @Test
    fun `can parse hardcoded`() {
        val rateProvider = TabularExchangeRateProvider.hardcoded()
        assertThat(rateProvider.rateAt(LocalDate.parse("2021-05-16")), `is`(21.024))
        assertThat(rateProvider.rateAt(LocalDate.parse("2021-05-14")), `is`(21.024))
        assertThat(rateProvider.rateAt(LocalDate.parse("2022-05-15")), `is`(23.825))
        assertThat(rateProvider.rateAt(LocalDate.parse("2023-05-15")), `is`(21.671))
        assertThat(rateProvider.rateAt(LocalDate.parse("2024-05-15")), `is`(22.861))
    }
}