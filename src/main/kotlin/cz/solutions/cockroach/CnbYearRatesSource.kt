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
 * Bundled snapshots are authoritative for completed years (CNB never amends
 * past fixings) and let the tool run offline / reproducibly for those years.
 * Used by tests, by [TabularExchangeRateProvider.hardcoded] and as the
 * preferred branch of [ClasspathOrHttpCnbYearRatesSource].
 */
class ClasspathCnbYearRatesSource : CnbYearRatesSource {
    override fun loadYear(year: Int): List<String> = resourceNames(year).map { loadResource(it) }

    /** True iff every resource needed for [year] is present on the classpath. */
    fun hasYear(year: Int): Boolean = resourceNames(year).all {
        ClasspathCnbYearRatesSource::class.java.getResource(it) != null
    }

    private fun resourceNames(year: Int): List<String> = when (year) {
        2022 -> listOf("rates_2022_a.txt", "rates_2022_b.txt")
        else -> listOf("rates_$year.txt")
    }

    private fun loadResource(name: String): String {
        return ClasspathCnbYearRatesSource::class.java.getResourceAsStream(name)?.use {
            it.reader(StandardCharsets.UTF_8).readText()
        } ?: throw IllegalStateException("classpath resource not found: $name")
    }
}

/**
 * Prefers a classpath-bundled snapshot for years that ship with the
 * release (offline, reproducible) and only consults [http] for years not
 * bundled — typically the current/future year, or anything past the most
 * recent release. This means a report for a completed past year does not
 * require network access and survives CNB website outages.
 */
class ClasspathOrHttpCnbYearRatesSource(
    private val http: CnbYearRatesSource,
    private val classpath: ClasspathCnbYearRatesSource = ClasspathCnbYearRatesSource(),
) : CnbYearRatesSource {

    override fun loadYear(year: Int): List<String> {
        if (classpath.hasYear(year)) {
            LOGGER.fine("using bundled CNB rates for $year")
            return classpath.loadYear(year)
        }
        return http.loadYear(year)
    }

    companion object {
        private val LOGGER = Logger.getLogger(ClasspathOrHttpCnbYearRatesSource::class.java.name)
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

    private fun downloadFresh(year: Int): String = download(year, cached = false)

    private fun downloadToFile(year: Int, target: File) {
        val content = download(year, cached = true)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(content, StandardCharsets.UTF_8)
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("could not replace cached file ${target.absolutePath}")
        }
        if (!tmp.renameTo(target)) {
            throw IllegalStateException("could not move ${tmp.absolutePath} to ${target.absolutePath}")
        }
    }

    private fun download(year: Int, cached: Boolean): String {
        val url = URI("$baseUrl?year=$year").toURL()
        val suffix = if (cached) "" else " (incomplete year, not cached)"
        LOGGER.info("downloading CNB rates for $year from $url$suffix")
        val connection = url.openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        val content = connection.getInputStream().use { it.reader(StandardCharsets.UTF_8).readText() }
        require(content.lines().any { isHeaderLine(it) }) {
            "CNB response for $year does not contain a 'Date|' / 'Datum|' header line; refusing to use it. " +
                    "First 200 chars: '${content.take(200).replace("\n", "\\n")}'"
        }
        return content
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

        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 60_000

        const val DEFAULT_BASE_URL =
            "https://www.cnb.cz/en/financial-markets/foreign-exchange-market/central-bank-exchange-rate-fixing/central-bank-exchange-rate-fixing/year.txt"

        fun defaultCacheDir(): File {
            val home = System.getProperty("user.home") ?: "."
            return File(home, ".cache/cockroach/rates")
        }
    }
}
