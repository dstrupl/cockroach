package com.cisco.td.general.cocroach;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReportGenerator {
    public Report generateForYear(ParsedExport parsedExport, int year, ExchangeRateProvider exchangeRateProvider) {
        DateInterval interval = DateInterval.year(year);

        return new Report(
                generateRsuReport(parsedExport, interval, exchangeRateProvider),
                generateDividendReport(parsedExport, interval, exchangeRateProvider),
                generateEsppReport(parsedExport, interval, exchangeRateProvider),
                generateSalesReport(parsedExport, interval, exchangeRateProvider)
        );
    }

    private SalesReport generateSalesReport(ParsedExport parsedExport, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {
        SalesReportPreparation salesReportPreparation = new SalesReportPreparation();
        return salesReportPreparation.generateSalesReport(parsedExport.getSaleRecords(), interval, exchangeRateProvider);
    }

    private EsppReport generateEsppReport(ParsedExport parsedExport, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {
        EsppReportPreparation esppReportPreparation = new EsppReportPreparation();
        return esppReportPreparation.generateEsppReport(parsedExport.getEsppRecords(), interval, exchangeRateProvider);
    }

    private RsuReport generateRsuReport(ParsedExport parsedExport, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {
        RsuReportPreparation rsuReportPreparation = new RsuReportPreparation();
        return rsuReportPreparation.generateRsuReport(parsedExport.getRsuRecords(), interval, exchangeRateProvider);
    }

    private DividendReport generateDividendReport(ParsedExport parsedExport, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {
        DividentReportPreparation dividentReportPreparation = new DividentReportPreparation();
        return dividentReportPreparation.generateDividendReport(parsedExport.getDividendRecords(), parsedExport.getTaxRecords(), parsedExport.getTaxReversalRecords(), interval, exchangeRateProvider);
    }
}
