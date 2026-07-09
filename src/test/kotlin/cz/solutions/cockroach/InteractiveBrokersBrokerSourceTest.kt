package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InteractiveBrokersBrokerSourceTest {

    @Test
    fun parsesConfiguredDividendsDirectory(@TempDir tempDir: File) {
        val dividendsDir = File(tempDir, "dividends")
        check(dividendsDir.mkdirs())
        val export = File(dividendsDir, "transactions.csv")
        requireNotNull(javaClass.getResourceAsStream("ib_dividend.csv")).use { input ->
            export.outputStream().use { input.copyTo(it) }
        }

        val parsed = InteractiveBrokersBrokerSource(tempDir).parse()

        assertThat(parsed.dividendRecords).hasSize(2)
        assertThat(parsed.taxRecords).hasSize(2)
        assertThat(parsed.dividendRecords).allSatisfy {
            assertThat(it.broker).isEqualTo("Interactive Brokers")
            assertThat(it.country).isEqualTo("US")
            assertThat(it.currency).isEqualTo(Currency.USD)
        }
    }
}
