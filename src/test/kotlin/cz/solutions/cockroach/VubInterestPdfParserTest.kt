package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class VubInterestPdfParserTest {

    companion object {
        // Real VÚB statement bundled in input/ for end-to-end runs; tests using it
        // are skipped when the file is absent (e.g. CI without private samples).
        private val EXTERNAL_PDF =
            File("input/2025/statement_account-VUB-2025.pdf")
    }

    @Test
    fun extractStatementYearPicksClosingBalanceYear() {
        val text = """
            ACCOUNT STATEMENT
            Currency: CZK
            Account balance as at 31/12/2024
            1.000,00
            Account balance as at 31/12/2025
            2.000,00
        """.trimIndent()

        val year = VubInterestPdfParser.extractStatementYear(text, "fixture.pdf")

        assertThat(year).isEqualTo(2025)
    }

    @Test
    fun extractStatementYearThrowsWhenNoBalanceLineFound() {
        val text = "ACCOUNT STATEMENT\nCurrency: CZK\n"

        assertThatIllegalStateException()
            .isThrownBy { VubInterestPdfParser.extractStatementYear(text, "broken.pdf") }
            .withMessageContaining("cannot determine statement year")
    }

    @Test
    fun parseFailsLoudlyWhenConfiguredYearDiffersFromStatementYear() {
        assumeTrue(EXTERNAL_PDF.exists(), "External VÚB PDF not available, skipping")

        assertThatIllegalStateException()
            .isThrownBy { VubInterestPdfParser.parse(EXTERNAL_PDF, year = 2024) }
            .withMessageContaining("covers year 2025")
            .withMessageContaining("configured tax year is 2024")
    }

    @Test
    fun parseSucceedsWhenConfiguredYearMatchesStatementYear() {
        assumeTrue(EXTERNAL_PDF.exists(), "External VÚB PDF not available, skipping")

        val records = VubInterestPdfParser.parse(EXTERNAL_PDF, year = 2025)

        assertThat(records).isNotEmpty
        assertThat(records).allSatisfy { r ->
            assertThat(r.date.year).isEqualTo(2025)
            assertThat(r.currency).isEqualTo(Currency.CZK)
            assertThat(r.broker).isEqualTo("VÚB")
            assertThat(r.amount).isPositive
        }
    }
}
