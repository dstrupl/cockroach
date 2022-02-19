package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.time.TimeInterval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;
import static com.cognitivesecurity.commons.util.Literals.map;

public class EsppReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public Map<String, ?> generateEsppReport(List<EsppRecord> esppRecordList, TimeInterval interval, Double exchange) {
        List<EsppRecord> esppRecords = MoreFluentIterable.from(esppRecordList)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(EsppRecord::getDate))
                .toList();


        ArrayList<PrintableEspp> printableEsppList = new ArrayList<>();
        double profitDolarValue = 0;
        double profitCroneValue = 0;
        int totalEsppAmount = 0;

        for (EsppRecord espp : esppRecords) {
            double partialProfit = espp.getPurchaseFmv() - espp.getPurchasePrice();
            profitDolarValue += espp.getQuantity() * partialProfit;
            profitCroneValue += espp.getQuantity() * partialProfit * exchange;

            totalEsppAmount += espp.getQuantity();

            printableEsppList.add(
                    new PrintableEspp(
                            DATE_FORMATTERTER.print(espp.getDate()),
                            espp.getQuantity(),
                            formatDouble(espp.getPurchasePrice()),
                            formatDouble(espp.getPurchaseFmv()),
                            formatDouble(partialProfit),
                            formatDouble(partialProfit * espp.getQuantity()),
                            formatDouble(partialProfit * espp.getQuantity() * exchange)
                    )
            );
        }

        return map(
                "esppList", printableEsppList,
                "profitCroneValue", formatDouble(profitCroneValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "exchange", exchange,
                "totalAmount", totalEsppAmount
        );
    }
}
