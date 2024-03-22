package cz.solutions.cockroach;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReportGenerator {

    private final SalesReportPreparation salesReportPreparation = new SalesReportPreparation();
    private final EsppReportPreparation esppReportPreparation = new EsppReportPreparation();
    private final RsuReportPreparation rsuReportPreparation = new RsuReportPreparation();
    private final DividentReportPreparation dividentReportPreparation = new DividentReportPreparation();

    public Report generateForYear(ParsedExport parsedExport, int year, ExchangeRateProvider exchangeRateProvider) {
        DateInterval interval = DateInterval.year(year);

        return new Report(
                rsuReportPreparation.generateRsuReport(
                        parsedExport.getRsuRecords(),
                        interval,
                        exchangeRateProvider
                ),
                dividentReportPreparation.generateDividendReport(
                        parsedExport.getDividendRecords(),
                        parsedExport.getTaxRecords(),
                        parsedExport.getTaxReversalRecords(),
                        interval,
                        exchangeRateProvider
                ),
                esppReportPreparation.generateEsppReport(
                        parsedExport.getEsppRecords(),
                        interval,
                        exchangeRateProvider
                ),
                salesReportPreparation.generateSalesReport(
                        parsedExport.getSaleRecords(),
                        interval,
                        exchangeRateProvider
                )
        );
    }
}
