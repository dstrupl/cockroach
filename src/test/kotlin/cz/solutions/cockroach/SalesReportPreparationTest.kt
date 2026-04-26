package cz.solutions.cockroach

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class SalesReportPreparationTest {

    @Test
    fun `old purchases still taken account for 100K limit`() {
        val salesReport = SalesReportPreparation.generateSalesReport(
            listOf(
                SaleRecord(
                    LocalDate.parse("2021-06-30"),
                    "ESPP",
                    20.0,
                    50.0,
                    30.0,
                    40.0,
                    LocalDate.parse("2020-06-30"),
                    null
                ),
                SaleRecord(
                    LocalDate.parse("2021-06-30"),
                    "ESPP",
                    400.0,
                    40.0,
                    10.0,
                    30.0,
                    LocalDate.parse("2017-06-30"),
                    null
                )
            ),
            DateInterval.year(2021),
            YearConstantExchangeRateProvider.usdOnly(mapOf(
                2017 to 10.0,
                2018 to 10.0,
                2019 to 10.0,
                2020 to 10.0,
                2021 to 10.0
            ))
        )
        assertThat(salesReport.sellCroneValue, `is`(170000.0000))
        assertThat(salesReport.profitForTax, `is`(2000.0))
    }

    @Test
    fun `loss in last 3 years is distracted from profit`() {
        val salesReport = SalesReportPreparation.generateSalesReport(
            listOf(
                SaleRecord(
                    LocalDate.parse("2021-06-30"),
                    "ESPP",
                    20.0,
                    50.0,
                    30.0,
                    40.0,
                    LocalDate.parse("2020-06-30"),
                    null
                ),
                SaleRecord(
                    LocalDate.parse("2021-06-15"),
                    "ESPP",
                    40.0,
                    40.0,
                    10.0,
                    41.0,
                    LocalDate.parse("2021-05-30"),
                    null
                )
            ),
            DateInterval.year(2021),
            YearConstantExchangeRateProvider.usdOnly(mapOf(
                2021 to 10.0,
                2020 to 10.0
            ))
        )
        assertThat(salesReport.sellCroneValue, `is`(26000.0000))
        assertThat(salesReport.recentProfitCroneValue, `is`(1600.0000))
        assertThat(salesReport.profitForTax, `is`(0.0))
    }
}