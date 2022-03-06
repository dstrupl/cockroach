package com.cisco.td.general.cocroach;

import com.cisco.td.ade.generate.templating.GeneralTemplateHelpers;
import com.cisco.td.ade.generate.templating.Template;
import com.cisco.td.ade.generate.templating.TemplateEngine;
import com.cognitivesecurity.commons.time.TimeInterval;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Map;

import static com.cognitivesecurity.commons.util.Literals.map;

public class ReportGenerator {


    public Report generateForYear(ParsedExport parsedExport, int year) {
        TimeInterval interval = new TimeInterval(
                new DateTime(year, 1, 1, 0, 0, DateTimeZone.UTC).getMillis(),
                new DateTime(year + 1, 1, 1, 0, 0, DateTimeZone.UTC).getMillis()
        );


        ExchangeRateProvider exchangeRateProvider = YearConstantExchangeRateProvider.hardcoded();


        return new Report(
                generateRsuReport(parsedExport, interval, exchangeRateProvider),
                generateDividendReport(parsedExport, interval, exchangeRateProvider),
                generateEsppReport(parsedExport, interval, exchangeRateProvider),
                generateSalesReport(parsedExport, interval, exchangeRateProvider)
        );


    }

    private SalesReport generateSalesReport(ParsedExport parsedExport, TimeInterval interval, ExchangeRateProvider exchangeRateProvider) {
        SalesReportPreparation salesReportPreparation = new SalesReportPreparation();
        return salesReportPreparation.generateSalesReport(parsedExport.getSaleRecords(),interval,exchangeRateProvider);
    }

    private EsppReport generateEsppReport(ParsedExport parsedExport, TimeInterval interval, ExchangeRateProvider exchangeRateProvider) {
        EsppReportPreparation esppReportPreparation = new EsppReportPreparation();
        return esppReportPreparation.generateEsppReport(parsedExport.getEsppRecords(),interval,exchangeRateProvider);
    }

    private RsuReport generateRsuReport(ParsedExport parsedExport, TimeInterval interval, ExchangeRateProvider exchangeRateProvider) {
        RsuReportPreparation rsuReportPreparation = new RsuReportPreparation();
        return rsuReportPreparation.generateRsuReport(parsedExport.getRsuRecords(), interval, exchangeRateProvider);
    }

    private DividendReport generateDividendReport(ParsedExport parsedExport, TimeInterval interval,ExchangeRateProvider exchangeRateProvider) {
        DividentReportPreparation dividentReportPreparation = new DividentReportPreparation();
        return dividentReportPreparation.generateDividendReport(parsedExport.getDividendRecords(), parsedExport.getTaxRecords(), parsedExport.getTaxReversalRecords(),interval,exchangeRateProvider);
    }

}
