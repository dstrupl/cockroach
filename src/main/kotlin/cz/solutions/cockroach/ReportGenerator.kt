package cz.solutions.cockroach

object ReportGenerator {

    /**
     * Year of the Czech RSU/ESPP taxation legislative cutover. The "..._2024" reports compare the
     * old vs. new methodology for unsold equity granted before this year and are emitted alongside
     * every per-year report. Centralised so the value appears in exactly one place.
     */
    const val LEGISLATIVE_TRANSITION_YEAR = 2024

    fun generateForYear(parsedExport: ParsedExport, year: Int, exchangeRateProvider: ExchangeRateProvider): Report {
        val interval = DateInterval.year(year)

        return Report(
            rsuReport = RsuReportPreparation.generateRsuReport(
                parsedExport.rsuRecords,
                parsedExport.saleRecords,
                interval,
                exchangeRateProvider
            ),
            dividendReport = DividentReportPreparation.generateDividendReport(
                parsedExport.dividendRecords,
                parsedExport.taxRecords,
                parsedExport.taxReversalRecords,
                interval,
                exchangeRateProvider
            ),
            interestReport = InterestReportPreparation.generateInterestReport(
                parsedExport.interestRecords,
                interval,
                exchangeRateProvider
            ),
            esppReport = EsppReportPreparation.generateEsppReport(
                parsedExport.esppRecords,
                parsedExport.saleRecords,
                interval,
                exchangeRateProvider
            ),
            salesReport = SalesReportPreparation.generateSalesReport(
                parsedExport.saleRecords,
                interval,
                exchangeRateProvider
            ),
            rsuReport2024 = RsuReportPreparation.generateRsuReport(
                parsedExport.rsuRecords,
                parsedExport.saleRecords,
                DateInterval.year(LEGISLATIVE_TRANSITION_YEAR),
                exchangeRateProvider,
                {quantity, soldQuantity -> quantity-soldQuantity}
            ),

            esppReport2024 = EsppReportPreparation.generateEsppReport(
                parsedExport.esppRecords,
                parsedExport.saleRecords,
                DateInterval.year(LEGISLATIVE_TRANSITION_YEAR),
                exchangeRateProvider,
                {quantity, soldQuantity -> quantity-soldQuantity}
            ),
        )
    }
}