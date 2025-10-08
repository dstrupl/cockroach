package cz.solutions.cockroach

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.joda.time.LocalDate
import org.junit.Test

internal class DateIntervalTest {
    @Test
    fun `contains correct dates`() {
        val interval = DateInterval.year(2021)
        assertThat(interval.contains(LocalDate(2020, 12, 31)), `is`(false))
        assertThat(interval.contains(LocalDate(2021, 1, 1)), `is`(true))
        assertThat(interval.contains(LocalDate(2021, 6, 14)), `is`(true))
        assertThat(interval.contains(LocalDate(2021, 12, 31)), `is`(true))
        assertThat(interval.contains(LocalDate(2022, 1, 1)), `is`(false))
    }
}