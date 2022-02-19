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
    private static final Map<Integer, Double> EXCHANGE = map(
            2018, 21.780,
            2019, 22.930,
            2020, 23.140,
            2021, 21.72
    );

    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    private final Template rsuTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("rsu.hbs");
    private final Template dividendTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("dividend.hbs");
    private final Template esppTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("espp.hbs");
    private final Template salesTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("sales.hbs");


    public Report generateForYear(ParsedExport parsedExport, int year) {
        TimeInterval interval = new TimeInterval(
                new DateTime(year, 1, 1, 0, 0, DateTimeZone.UTC).getMillis(),
                new DateTime(year + 1, 1, 1, 0, 0, DateTimeZone.UTC).getMillis()
        );

        Double exchange = EXCHANGE.get(year);






        return new Report(
                generateRsuReport(parsedExport, interval, exchange),
                generateDividendReport(parsedExport, interval, exchange),
                generateEsppReport(parsedExport, interval, exchange),
                generateSalesReport(parsedExport, interval, exchange)
        );


    }

    private String generateSalesReport(ParsedExport parsedExport, TimeInterval interval, Double exchange) {
        SalesReportPreparation salesReportPreparation = new SalesReportPreparation();
        return salesTemplate.render(salesReportPreparation.generateSalesReport(parsedExport.getSaleRecords(),interval,exchange));
    }

    private String generateEsppReport(ParsedExport parsedExport, TimeInterval interval, Double exchange) {
        EsppReportPreparation esppReportPreparation = new EsppReportPreparation();
        return esppTemplate.render(esppReportPreparation.generateEsppReport(parsedExport.getEsppRecords(),interval,exchange));
    }

    private String generateRsuReport(ParsedExport parsedExport, TimeInterval interval, Double exchange) {
        RsuReportPreparation rsuReportPreparation = new RsuReportPreparation();
        return rsuTemplate.render(rsuReportPreparation.generateRsuReport(parsedExport.getRsuRecords(),interval,exchange));
    }

    private String generateDividendReport(ParsedExport parsedExport, TimeInterval interval, Double exchange) {
        DividentReportPreparation dividentReportPreparation = new DividentReportPreparation();
        return dividendTemplate.render(dividentReportPreparation.generateDividendReport(parsedExport.getDividendRecords(), parsedExport.getTaxRecords(), parsedExport.getTaxReversalRecords(),interval,exchange));
    }

}
