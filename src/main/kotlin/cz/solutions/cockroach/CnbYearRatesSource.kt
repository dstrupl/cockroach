package cz.solutions.cockroach

import org.joda.time.LocalDate
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

/**
 * Source of CNB exchange-rate-fixing data for a whole year.
 *
 * A single year may produce more than one chunk: the column layout of the
 * CNB year.txt file occasionally changes mid-year (e.g. 2022 when HRK was
 * removed). Each returned string is a self-contained chunk that starts with
 * its own header line.
 */
interface CnbYearRatesSource {
    fun loadYear(year: Int): List<String>
}

/**
 * Reads CNB year.txt content from bundled classpath resources.
 *
 * Used by tests and by [TabularExchangeRateProvider.hardcoded] so that no
 * network is touched during unit tests.
 */
class ClasspathCnbYearRatesSource : CnbYearRatesSource {
    override fun loadYear(year: Int): List<String> {
        return when (year) {
            2022 -> listOf(loadResource("rates_2022_a.txt"), loadResource("rates_2022_b.txt"))
            else -> listOf(loadResource("rates_$year.txt"))
        }
    }

    private fun loadResource(name: String): String {
        return ClasspathCnbYearRatesSource::class.java.getResourceAsStream(name)?.use {
            it.reader(StandardCharsets.UTF_8).readText()
        } ?: throw IllegalStateException("classpath resource not found: $name")
    }
}

/**
 * Downloads CNB year.txt from cnb.cz on first use and caches completed
 * past years on disk under [cacheDir]. CNB never amends fixings
 * retroactively, but the last fixing of a year (and any late corrections)
 * may take a few business days to be published. A year N is therefore
 * only treated as complete – and eligible for permanent caching – once
 * [today] is at least [safeDaysAfterYearEnd] days into year N+1. The
 * current year (and any future year) is always downloaded fresh and
 * never written to disk, since its fixing series is still growing.
 *
 * When the downloaded content contains more than one header line (e.g. the
 * 2022 HRK transition), it is split into multiple chunks so downstream
 * parsing keeps working.
 */
class HttpCnbYearRatesSource(
    private val cacheDir: File,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val safeDaysAfterYearEnd: Int = 30,
    private val today: () -> LocalDate = { LocalDate.now() }
) : CnbYearRatesSource {

    override fun loadYear(year: Int): List<String> {
        val raw = loadRaw(year)
        return splitByHeader(raw)
    }

    private fun loadRaw(year: Int): String {
        if (!isYearComplete(year)) {
            return downloadFresh(year)
        }
        if (!cacheDir.exists()) {
            require(cacheDir.mkdirs() || cacheDir.exists()) {
                "could not create cache directory ${cacheDir.absolutePath}"
            }
        }
        val cacheFile = File(cacheDir, "rates_$year.txt")
        if (!cacheFile.exists()) {
            downloadToFile(year, cacheFile)
        }
        return cacheFile.readText(StandardCharsets.UTF_8)
    }

    private fun isYearComplete(year: Int): Boolean {
        val safeAfter = LocalDate(year + 1, 1, 1).plusDays(safeDaysAfterYearEnd)
        return !today().isBefore(safeAfter)
    }

    private fun downloadFresh(year: Int): String {
        val url = URI("$baseUrl?year=$year").toURL()
        LOGGER.info("downloading CNB rates for $year from $url (incomplete year, not cached)")
        return url.openStream().use { it.reader(StandardCharsets.UTF_8).readText() }
    }

    private fun downloadToFile(year: Int, target: File) {
        val url = URI("$baseUrl?year=$year").toURL()
        LOGGER.info("downloading CNB rates for $year from $url")
        val tmp = File(target.parentFile, "${target.name}.tmp")
        url.openStream().use { ins ->
            tmp.outputStream().use { out -> ins.copyTo(out) }
        }
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("could not replace cached file ${target.absolutePath}")
        }
        if (!tmp.renameTo(target)) {
            throw IllegalStateException("could not move ${tmp.absolutePath} to ${target.absolutePath}")
        }
    }

    private fun splitByHeader(content: String): List<String> {
        val chunks = mutableListOf<MutableList<String>>()
        for (line in content.lines()) {
            if (isHeaderLine(line)) {
                chunks.add(mutableListOf(line))
            } else if (line.isNotBlank() && chunks.isNotEmpty()) {
                chunks.last().add(line)
            }
        }
        require(chunks.isNotEmpty()) { "no header line found in CNB response" }
        return chunks.map { it.joinToString("\n") }
    }

    private fun isHeaderLine(line: String): Boolean {
        val first = line.substringBefore('|').trim()
        return first == "Date" || first == "Datum"
    }

    companion object {
        private val LOGGER = Logger.getLogger(HttpCnbYearRatesSource::class.java.name)

        const val DEFAULT_BASE_URL =
            "https://www.cnb.cz/en/financial-markets/foreign-exchange-market/central-bank-exchange-rate-fixing/central-bank-exchange-rate-fixing/year.txt"

        fun defaultCacheDir(): File {
            val home = System.getProperty("user.home") ?: "."
            return File(home, ".cache/cockroach/rates")
        }
    }
}
