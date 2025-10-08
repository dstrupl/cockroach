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
            )
        )
    }
}