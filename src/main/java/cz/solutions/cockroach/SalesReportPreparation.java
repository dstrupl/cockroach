package cz.solutions.cockroach;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SalesReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public SalesReport generateSalesReport(List<SaleRecord> salesRecords, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {

        List<SaleRecord> saleRecords = salesRecords.stream()
                .filter(a -> interval.contains(a.getDate()))
                .sorted(Comparator.comparing(SaleRecord::getDate))
                .toList();

        List<PrintableSale> printableSalesList = new ArrayList<>();
        double sellDollarValue = 0;
        double sellCroneValue = 0;
        double profitDolarValue = 0;
        double recentProfitCroneValue = 0;
        double recentSellCroneValue = 0;
        double recentBuyCroneValue = 0;

        double totalAmount = 0;

        for (SaleRecord sale : saleRecords) {
            double exchange = exchangeRateProvider.rateAt(sale.getDate());

            double partialsellDolarValue = sale.getQuantity() * sale.getSalePrice();
            double partialsellCroneValue = partialsellDolarValue * exchange;
            double buyPriceDolarValue = sale.getQuantity() * sale.getPurchaseFmv();

            double partialProfitValue = partialsellDolarValue - buyPriceDolarValue;
            double partialRecentProfitValue = sale.isTaxable() ? partialProfitValue : 0;
            double partialRecentProfitCroneValue = partialRecentProfitValue * exchange;

            sellDollarValue += partialsellDolarValue;
            sellCroneValue += partialsellCroneValue;

            profitDolarValue += partialProfitValue;
            recentProfitCroneValue += partialRecentProfitCroneValue;

            if(sale.isTaxable()){
                recentSellCroneValue += partialsellCroneValue;
                recentBuyCroneValue += buyPriceDolarValue * exchange;
            }
            totalAmount += sale.getQuantity();


            printableSalesList.add(
                    new PrintableSale(
                            DATE_FORMATTERTER.print(sale.getDate()),
                            DATE_FORMATTERTER.print(sale.getPurchaseDate()),
                            FormatingHelper.formatDouble(sale.getQuantity()),
                            FormatingHelper.formatExchangeRate(exchange),

                            FormatingHelper.formatDouble(sale.getPurchaseFmv()),
                            FormatingHelper.formatDouble(sale.getSalePrice()),
                            FormatingHelper.formatDouble(sale.getSalePrice() - sale.getPurchaseFmv()),

                            FormatingHelper.formatDouble(partialsellDolarValue),
                            FormatingHelper.formatDouble(partialProfitValue),
                            FormatingHelper.formatDouble(partialsellCroneValue),
                            FormatingHelper.formatDouble(partialRecentProfitCroneValue)
                    )
            );
        }

        double profitForTax;
        double sellCroneForTax;
        double buyCroneForTax;
        if (sellCroneValue < 100000) {
            // no need to pay taxes
            profitForTax = 0;
            sellCroneForTax = 0;
            buyCroneForTax=0;
        } else {
            //pay taxes only from items bought in last 3 years
            profitForTax = recentProfitCroneValue;
            sellCroneForTax = recentSellCroneValue;
            buyCroneForTax = recentBuyCroneValue;
        }
        return new SalesReport(
                printableSalesList,
                sellCroneValue,
                sellDollarValue,
                profitDolarValue,
                recentProfitCroneValue,
                totalAmount,
                profitForTax,
                sellCroneForTax,
                buyCroneForTax
        );


    }
}
