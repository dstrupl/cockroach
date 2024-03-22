package cz.solutions.cockroach;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EsppReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public EsppReport generateEsppReport(List<EsppRecord> esppRecordList, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {
        List<EsppRecord> esppRecords = esppRecordList.stream()
                .filter(a -> interval.contains(a.getPurchaseDate()))
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
                            FormatingHelper.formatExchangeRate(exchange),
                            FormatingHelper.formatDouble(espp.getPurchasePrice()),
                            FormatingHelper.formatDouble(espp.getPurchaseFmv()),
                            FormatingHelper.formatDouble(partialProfit),
                            FormatingHelper.formatDouble(partialProfit * espp.getQuantity()),
                            FormatingHelper.formatDouble(partialProfit * espp.getQuantity() * exchange)
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
