package cz.solutions.cockroach

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class CnbYearRatesSourceTest {

    private val classpath = ClasspathCnbYearRatesSource()

    @Test
    fun `classpath source reports bundled years`() {
        assertThat(classpath.hasYear(2021), `is`(true))
        assertThat(classpath.hasYear(2022), `is`(true))
        assertThat(classpath.hasYear(2025), `is`(true))
        assertThat(classpath.hasYear(1999), `is`(false))
        assertThat(classpath.hasYear(2099), `is`(false))
    }

    @Test
    fun `composite prefers bundled snapshot for completed year`() {
        val http = RecordingSource()
        val composite = ClasspathOrHttpCnbYearRatesSource(http = http)

        val chunks = composite.loadYear(2025)

        assertThat(chunks.size >= 1, `is`(true))
        assertThat(http.calls, `is`(emptyList()))
    }

    @Test
    fun `composite delegates to http for years not bundled`() {
        val http = RecordingSource(response = listOf("Date|1 USD\n01.01.2099|25.000"))
        val composite = ClasspathOrHttpCnbYearRatesSource(http = http)

        val chunks = composite.loadYear(2099)

        assertThat(chunks, contains("Date|1 USD\n01.01.2099|25.000"))
        assertThat(http.calls, contains(2099))
    }

    @Test
    fun `composite propagates http failure for years not bundled`() {
        val composite = ClasspathOrHttpCnbYearRatesSource(http = FailingSource())

        assertThrows(IllegalStateException::class.java) { composite.loadYear(2099) }
    }

    private class RecordingSource(private val response: List<String> = listOf("stub")) : CnbYearRatesSource {
        val calls = mutableListOf<Int>()
        override fun loadYear(year: Int): List<String> {
            calls.add(year)
            return response
        }
    }

    private class FailingSource : CnbYearRatesSource {
        override fun loadYear(year: Int): List<String> = throw IllegalStateException("network down")
    }
}
