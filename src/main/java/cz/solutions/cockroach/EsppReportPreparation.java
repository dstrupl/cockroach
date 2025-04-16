package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EsppReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public EsppReport generateEsppReport(List<EsppRecord> esppRecordList,
                                         List<SaleRecord> saleRecordList,
                                         DateInterval interval,
                                         ExchangeRateProvider exchangeRateProvider) {
        List<EsppRecord> esppRecords = esppRecordList.stream()
                .filter(a -> interval.contains(a.getPurchaseDate()))
                .sorted(Comparator.comparing(EsppRecord::getDate))
                .toList();

        List<SaleRecord> esppSaleRecords = saleRecordList.stream()
                .filter(a -> interval.contains(a.getDate()))
                .filter(a->a.getType().equals("ESPP"))
                .sorted(Comparator.comparing(SaleRecord::getDate))
                .toList();


        ArrayList<PrintableEspp> printableEsppList = new ArrayList<>();
        double profitDolarValue = 0;
        double profitCroneValue = 0;
        double taxablePofitCroneValue = 0;
        int totalEsppAmount = 0;

        for (EsppRecord espp : esppRecords) {
            double soldQuantity = esppSaleRecords.stream()
                    .filter(it -> espp.getPurchaseDate().equals(it.getPurchaseDate()))
                    .mapToDouble(SaleRecord::getQuantity)
                    .sum();

            EsppInfo esppInfo = withConvertedPrices(espp,soldQuantity,exchangeRateProvider);


            printableEsppList.add(esppInfo.toPrintable());
            profitDolarValue += esppInfo.getBuyProfitValue();
            profitCroneValue += esppInfo.getBuyCronePofitValue();

            totalEsppAmount += espp.getQuantity();
            taxablePofitCroneValue+=esppInfo.getTaxableBuyCronePofitValue();
        }

        return new EsppReport(
                printableEsppList,
                profitCroneValue,
                profitDolarValue,
                totalEsppAmount,
                taxablePofitCroneValue
        );
    }

        private EsppInfo  withConvertedPrices(EsppRecord espp, double soldAmount, ExchangeRateProvider exchangeRateProvider){
            double exchange = exchangeRateProvider.rateAt(espp.getPurchaseDate());
            double partialProfit = espp.getPurchaseFmv() - espp.getPurchasePrice();

            return new EsppInfo(
                    espp.getPurchaseDate(),
                    espp.getQuantity(),
                    exchange,
                    espp.getPurchasePrice(),
                    espp.getPurchaseFmv(),
                    partialProfit,
                    partialProfit * espp.getQuantity(),
                    partialProfit * espp.getQuantity() * exchange,
                    soldAmount,
                    partialProfit*soldAmount*exchange
            );
        }

        @Value
        private static class EsppInfo {
            LocalDate date;
            int amount;
            double exchange;
            double onePricePurchaseDolarValue;
            double onePriceDolarValue;
            double oneProfitValue;
            double buyProfitValue;
            double buyCronePofitValue;
            double soldAmount;
            double taxableBuyCronePofitValue;

            PrintableEspp toPrintable() {
                return new PrintableEspp(
                        DATE_FORMATTER.print(date),
                        amount,
                        FormatingHelper.formatExchangeRate(exchange),
                        FormatingHelper.formatDouble(onePricePurchaseDolarValue),
                        FormatingHelper.formatDouble(onePriceDolarValue),
                        FormatingHelper.formatDouble(oneProfitValue),
                        FormatingHelper.formatDouble(buyProfitValue),
                        FormatingHelper.formatDouble(buyCronePofitValue),
                        soldAmount,
                        FormatingHelper.formatDouble(taxableBuyCronePofitValue)
                );
            }
        }

}
