package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.time.TimeInterval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;

public class EsppReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public EsppReport generateEsppReport(List<EsppRecord> esppRecordList, TimeInterval interval, ExchangeRateProvider exchangeRateProvider) {
        List<EsppRecord> esppRecords = MoreFluentIterable.from(esppRecordList)
                .filter(a -> interval.includes(a.getPurchaseDate().getMillis()))
                .sorted(Comparator.comparing(EsppRecord::getDate))
                .toList();


        ArrayList<PrintableEspp> printableEsppList = new ArrayList<>();
        double profitDolarValue = 0;
        double profitCroneValue = 0;
        int totalEsppAmount = 0;

        for (EsppRecord espp : esppRecords) {
            double exchange = exchangeRateProvider.rateAt(espp.getPurchaseDate());
            double partialProfit = espp.getPurchaseFmv() - espp.getPurchasePrice();
            profitDolarValue += espp.getQuantity() * partialProfit;
            profitCroneValue += espp.getQuantity() * partialProfit * exchange;

            totalEsppAmount += espp.getQuantity();

            printableEsppList.add(
                    new PrintableEspp(
                            DATE_FORMATTERTER.print(espp.getPurchaseDate()),
                            espp.getQuantity(),
                            exchange,
                            formatDouble(espp.getPurchasePrice()),
                            formatDouble(espp.getPurchaseFmv()),
                            formatDouble(partialProfit),
                            formatDouble(partialProfit * espp.getQuantity()),
                            formatDouble(partialProfit * espp.getQuantity() * exchange)
                    )
            );
        }

        return new EsppReport(
                printableEsppList,
                profitCroneValue,
                profitDolarValue,
                totalEsppAmount
        );
    }
}
