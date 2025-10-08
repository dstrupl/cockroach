package cz.solutions.cockroach

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

object TemplateHelpers {
    private val DAYS_LOOKUP = (1..31).associate { it.toLong() to getOrdinal(it) }
    private val DEFAULT_DATE_FORMATTER = DateTimeFormatterBuilder()
        .appendPattern("eeee, LLLL d")
        .appendText(ChronoField.DAY_OF_MONTH, DAYS_LOOKUP)
        .toFormatter()
        .withZone(ZoneOffset.UTC)

    fun upperCase(input: Any): String {
        return input.toString().uppercase()
    }

    fun lowerCase(input: Any): String {
        return input.toString().lowercase()
    }

    fun formatDefaultDate(instant: Instant): String {
        return DEFAULT_DATE_FORMATTER.format(instant)
    }

    private fun getOrdinal(n: Int): String {
        if (n in 11..13) {
            return "th"
        }
        return when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}