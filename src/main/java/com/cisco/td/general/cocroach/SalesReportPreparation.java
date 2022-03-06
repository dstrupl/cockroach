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

public class SalesReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();


    public Map<String,?> generateSalesReport(List<SaleRecord> salesRecords, TimeInterval interval, ExchangeRateProvider exchangeRateProvider) {

        List<SaleRecord> saleRecords = MoreFluentIterable.from(salesRecords)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(SaleRecord::getDate))
                .toList();

        List<PrintableSale> printableSalesList = new ArrayList<>();
        double sellDollarValue = 0;
        double sellCroneValue = 0;
        double profitDolarValue = 0;
        double recentProfitCroneValue = 0;
        int totalAmount = 0;

        for (SaleRecord sale : saleRecords) {
            double exchange = exchangeRateProvider.rateAt(sale.getDate());

            double partialsellDolarValue = sale.getQuantity() * sale.getSalePrice();
            double partialsellCroneValue = partialsellDolarValue * exchange;
            double buyPriceDolarValue = sale.getQuantity() * sale.getPurchaseFmv();

            double partialProfitValue = partialsellDolarValue - buyPriceDolarValue;
            double partialRecentProfitValue = sale.isTaxable() ? partialProfitValue : 0;
            double partialRecentProfitCroneValue = partialRecentProfitValue*exchange;

            sellDollarValue += partialsellDolarValue;
            sellCroneValue += partialsellCroneValue;

            profitDolarValue += partialProfitValue;
            recentProfitCroneValue += partialRecentProfitCroneValue;
            totalAmount += sale.getQuantity();




            printableSalesList.add(
                    new PrintableSale(
                            DATE_FORMATTERTER.print(sale.getDate()),
                            DATE_FORMATTERTER.print(sale.getPurchaseDate()),
                            sale.getQuantity(),
                            exchange,

                            formatDouble(sale.getPurchaseFmv()),
                            formatDouble(sale.getSalePrice()),
                            formatDouble(sale.getSalePrice() - sale.getPurchaseFmv()),

                            formatDouble(partialsellDolarValue),
                            formatDouble(partialProfitValue),
                            formatDouble(partialsellCroneValue),
                            formatDouble(partialRecentProfitCroneValue)
                    )
            );
        }

        String profitForTax;
        if (sellCroneValue< 100000) {
            // no need to pay taxes
            profitForTax = "";
        } else {
            //pay taxes only from items bought in last 3 years
            profitForTax=formatDouble(recentProfitCroneValue);
        }

        return map(
                "salesList", printableSalesList,
                "sellCroneValue", formatDouble(sellCroneValue),
                "sellDollarValue", formatDouble(sellDollarValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "profitRecentCroneValue", formatDouble(recentProfitCroneValue),
                "totalAmount", totalAmount,
                "profitForTax", profitForTax
        );

    }
}
