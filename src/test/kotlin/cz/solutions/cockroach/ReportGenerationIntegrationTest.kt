package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class ReportGenerationIntegrationTest {

    @Test
    fun rendersEveryPdfAndGuideFromCombinedBrokerData() {
        val date = LocalDate(2025, 6, 15)
        val parsed = ParsedExport(
            rsuRecords = listOf(rsuRecord(date, 2, 50.0, date, "G-1", symbol = "ACME")),
            esppRecords = listOf(esppRecord(date, 3.0, 40.0, 45.0, 50.0, date, symbol = "ACME")),
            dividendRecords = listOf(
                dividendRecord(date, 100.0, Currency.USD, "ACME", "TestBroker", "US"),
                dividendRecord(date, 200.0, Currency.CZK, "CEZ", "TestBroker", "CZ"),
            ),
            taxRecords = listOf(
                taxRecord(date, -15.0, Currency.USD, "ACME", "TestBroker"),
                taxRecord(date, -30.0, Currency.CZK, "CEZ", "TestBroker"),
            ),
            taxReversalRecords = emptyList(),
            saleRecords = listOf(
                saleRecord(date, "RS", 1.0, 60.0, 50.0, 50.0, date.minusYears(1), "G-1", "ACME", "TestBroker")
            ),
            journalRecords = emptyList(),
            interestRecords = listOf(
                interestRecord(date, 10.0, Currency.USD, "IE-FUND", "Revolut", 0.0, "IE"),
                interestRecord(date, 50.0, Currency.CZK, "SK-ACCOUNT", "VÚB", 0.0, "SK"),
            ),
        )
        val rates = ExchangeRateProvider { _, currency -> if (currency == Currency.CZK) 1.0 else 25.0 }

        val report = ReportGenerator.generateForYear(parsed, 2025, rates)

        listOf(
            report.getDividendPdf(),
            report.getInterestPdf(),
            report.getRsuPdf(),
            report.getEsppPdf(),
            report.getSalesPdf(),
            report.getRsu2024Pdf(),
            report.getEspp2024Pdf(),
        ).forEach { pdf ->
            assertThat(pdf).isNotEmpty
            assertThat(pdf.toString(Charsets.US_ASCII)).startsWith("%PDF-")
        }
        assertThat(report.getGuide()).contains("<html")
    }
}
