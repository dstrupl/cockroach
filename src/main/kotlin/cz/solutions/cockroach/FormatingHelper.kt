package cz.solutions.cockroach

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.round

object FormatingHelper {

    private val format = DecimalFormat("#,##0.00").apply {
        decimalFormatSymbols = DecimalFormatSymbols().apply {
            decimalSeparator = ','
            groupingSeparator = '.'
        }
    }

    fun formatDouble(d: Double): String {
        return format.format(d)
    }

    fun formatRounded(d: Double): String {
        return round(d).toString()
    }

    fun formatExchangeRate(d: Double): String {
        return d.toString().replace('.', ',')
    }
}