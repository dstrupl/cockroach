package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RsuReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public RsuReport generateRsuReport(List<RsuRecord> rsuRecordList, DateInterval interval,ExchangeRateProvider exchangeRateProvider) {
        List<RsuRecord> rsuRecords = MoreFluentIterable.from(rsuRecordList)
                .filter(a -> interval.contains(a.getVestDate()))
                .sorted(Comparator.comparing(RsuRecord::getDate))
                .toList();


        ArrayList<PrintableRsu> printableRsuList = new ArrayList<>();
        double rsuCroneValue = 0;
        double rsuDolarValue = 0;
        int totalAmount = 0;

        for (RsuRecord rsu : rsuRecords) {
            double exchange = exchangeRateProvider.rateAt(rsu.getVestDate());
            double partialRsuDolarValue = rsu.getQuantity() * rsu.getVestFmv();
            double partialRsuCroneValue = partialRsuDolarValue * exchange;
            rsuCroneValue += partialRsuCroneValue;
            rsuDolarValue += partialRsuDolarValue;
            totalAmount += rsu.getQuantity();

            printableRsuList.add(
                    new PrintableRsu(
                            DATE_FORMATTER.print(rsu.getVestDate()),
                            rsu.getQuantity(),
                            exchange,
                            String.format("%.4f", rsu.getVestFmv()),
                            String.format("%.4f", partialRsuDolarValue),
                            String.format("%.4f", partialRsuCroneValue)
                    )
            );
        }


        return new RsuReport(
                printableRsuList,
                rsuCroneValue,
                rsuDolarValue,
                totalAmount
        ) ;
    }
}
