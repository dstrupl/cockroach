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


    public Report generateForYear(ParsedExport parsedExport, int year) {
        TimeInterval interval = new TimeInterval(
                new DateTime(year, 1, 1, 0, 0, DateTimeZone.UTC).getMillis(),
                new DateTime(year + 1, 1, 1, 0, 0, DateTimeZone.UTC).getMillis()
        );

        Double exchange = EXCHANGE.get(year);

        List<RsuRecord> rsuRecords = MoreFluentIterable.from(parsedExport.getRsuRecords())
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(RsuRecord::getDate))
                .toList();


        ArrayList<Object> printableRsuList = new ArrayList<>();
        double rsuCroneValue = 0;
        double rsuDolarValue = 0;
        int totalAmount = 0;

        for (RsuRecord rsu : rsuRecords) {
            double partialRsuDolarValue = rsu.getQuantity() * rsu.getVestFmv();
            double partialRsuCroneValue = partialRsuDolarValue * exchange;
            rsuCroneValue += partialRsuCroneValue;
            rsuDolarValue += partialRsuDolarValue;
            totalAmount += rsu.getQuantity();

            printableRsuList.add(
                    new PrintableRsu(
                            DATE_FORMATTERTER.print(rsu.getDate()),
                            rsu.getQuantity(),
                            String.format("%.4f", rsu.getVestFmv()),
                            String.format("%.4f", partialRsuDolarValue),
                            String.format("%.4f", partialRsuCroneValue)
                    )
            );
        }

        String reportData = rsuTemplate.render(map(
                "rsuList", printableRsuList,
                "rsuCroneValue", String.format("%.4f", rsuCroneValue),
                "rsuDolarValue", String.format("%.4f", rsuDolarValue),
                "exchange", exchange,
                "totalAmount", totalAmount
        ));


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



        List<EsppRecord> esppRecords = MoreFluentIterable.from(parsedExport.getEsppRecords())
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(EsppRecord::getDate))
                .toList();


        ArrayList<PrintableEspp> printableEsppList = new ArrayList<>();
        double profitDolarValue = 0;
        double profitCroneValue = 0;
        int totalEsppAmount = 0;

        for (EsppRecord espp : esppRecords) {
            double partialProfit = espp.getPurchaseFmv()-espp.getPurchasePrice();
            profitDolarValue+= espp.getQuantity()*partialProfit;
            profitCroneValue+=espp.getQuantity()*partialProfit*exchange;

            totalEsppAmount += espp.getQuantity();

            printableEsppList.add(
                    new PrintableEspp(
                            DATE_FORMATTERTER.print(espp.getDate()),
                            espp.getQuantity(),
                            formatDouble(espp.getPurchasePrice()),
                            formatDouble(espp.getPurchaseFmv()),
                            formatDouble(partialProfit),
                            formatDouble(partialProfit*espp.getQuantity()),
                            formatDouble(partialProfit*espp.getQuantity()*exchange)
                    )
            );
        }

        String esppReportData = esppTemplate.render(map(
                "esppList", printableEsppList,
                "profitCroneValue", formatDouble(profitCroneValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "exchange", exchange,
                "totalAmount", totalEsppAmount
        ));


        return new Report(
                reportData,
                dividendReportData,
                esppReportData
        );


    }

    private String formatDouble(double d) {
        return String.format("%.4f", d);
    }
}
