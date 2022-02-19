package com.cisco.td.general.cocroach;

import com.cisco.td.ade.generate.templating.GeneralTemplateHelpers;
import com.cisco.td.ade.generate.templating.Template;
import com.cisco.td.ade.generate.templating.TemplateEngine;
import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.time.TimeInterval;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;
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




        List<DividendRecord> dividendRecords = MoreFluentIterable.from(parsedExport.getDividendRecords())
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(DividendRecord::getDate))
                .toList();

        Map<DateTime, TaxRecord> taxRecords = MoreFluentIterable.from(parsedExport.getTaxRecords())
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(TaxRecord::getDate))
                .fluentUniqueIndex(TaxRecord::getDate)
                .immutableCopy();

        List<TaxReversalRecord> taxReversalRecords = MoreFluentIterable.from(parsedExport.getTaxReversalRecords())
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(TaxReversalRecord::getDate))
                .toList();


        ArrayList<PrintableDividend> printableDividendList = new ArrayList<>();

        double totalBruttoDollar = 0;
        double totalTaxDollar = 0;
        double totalBruttoCrown = 0;
        double totalTaxCrown = 0;

        for (DividendRecord dividendRecord : dividendRecords) {
            TaxRecord taxRecord = taxRecords.get(dividendRecord.getDate());

            totalBruttoDollar += dividendRecord.getAmount();
            totalTaxDollar += taxRecord.getAmount();
            totalBruttoCrown += dividendRecord.getAmount() * exchange;
            totalTaxCrown += taxRecord.getAmount() * exchange;

            printableDividendList.add(
                    new PrintableDividend(
                            DATE_FORMATTERTER.print(dividendRecord.getDate()),
                            formatDouble(dividendRecord.getAmount()),
                            formatDouble(taxRecord.getAmount()),
                            formatDouble(exchange * dividendRecord.getAmount()),
                            formatDouble(exchange * taxRecord.getAmount())
                    )
            );
        }

        double totalTaxReversalDollar = 0;
        double totalTaxReversalCrown = 0;

        for (TaxReversalRecord taxReversalRecord : taxReversalRecords) {
            totalTaxReversalDollar += taxReversalRecord.getAmount();
            totalTaxReversalCrown += taxReversalRecord.getAmount() * exchange;
        }

        String dividendReportData = dividendTemplate.render(map(
                "dividendList", printableDividendList,
                "totalBruttoDollar", formatDouble(totalBruttoDollar),
                "totalTaxDollar", formatDouble(totalTaxDollar),
                "exchange", exchange,
                "totalBruttoCrown", formatDouble(totalBruttoCrown),
                "totalTaxCrown", formatDouble(totalTaxCrown),
                "totalTaxReversal", totalTaxReversalDollar > 0 ? formatDouble(totalTaxReversalDollar) : "",
                "totalTaxReversalCrown", totalTaxReversalCrown > 0 ? formatDouble(totalTaxReversalCrown) : ""
        ));



        return new Report(
                generateRsuReport(parsedExport, interval, exchange),
                dividendReportData,
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

}
