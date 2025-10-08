package cz.solutions.cockroach

import org.joda.time.LocalDate

data class DateInterval(val startInclusive: LocalDate, val endExclusive: LocalDate) {

    companion object {
        fun year(year: Int): DateInterval {
            return DateInterval(LocalDate(year, 1, 1), LocalDate(year + 1, 1, 1))
        }
    }

    fun contains(date: LocalDate): Boolean {
        return startInclusive == date || (startInclusive.isBefore(date) && date.isBefore(endExclusive))
    }
}