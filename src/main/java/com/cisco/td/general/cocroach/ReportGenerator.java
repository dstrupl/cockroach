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
            2020, 23.140
    );

    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    private final Template rsuTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("rsu.hbs");

    public String generateForYear(ParsedExport parsedExport, int year) {
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
            double partialRsuCroneValue =partialRsuDolarValue * exchange;
            rsuCroneValue += partialRsuCroneValue;
            rsuDolarValue+= partialRsuDolarValue;
            totalAmount += rsu.getQuantity();

                        printableRsuList.add(
                    new PrintableRsu(
                            DATE_FORMATTERTER.print(rsu.getDate()),
                            rsu.getQuantity(),
                            String.format("%.4f",rsu.getVestFmv()),
                            String.format("%.4f",partialRsuDolarValue),
                            String.format("%.4f",partialRsuCroneValue)
                    )
            );
        }

        String reportData = rsuTemplate.render(map(
                "rsuList", printableRsuList,
                "rsuCroneValue", String.format("%.4f",rsuCroneValue),
                "rsuDolarValue", String.format("%.4f",rsuDolarValue),
                "exchange", exchange,
                "totalAmount", totalAmount
        ));


        return reportData;


    }
}
