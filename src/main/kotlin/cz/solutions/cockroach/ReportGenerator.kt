package cz.solutions.cockroach

object ReportGenerator {

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
                DateInterval.year(2024),
                exchangeRateProvider,
                {quantity, soldQuantity -> quantity-soldQuantity}
            ),

            esppReport2024 = EsppReportPreparation.generateEsppReport(
                parsedExport.esppRecords,
                parsedExport.saleRecords,
                DateInterval.year(2024),
                exchangeRateProvider,
                {quantity, soldQuantity -> quantity-soldQuantity}
            ),
        )
    }
}