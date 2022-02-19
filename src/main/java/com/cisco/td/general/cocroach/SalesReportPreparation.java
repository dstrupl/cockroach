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


    public Map<String,?> generateSalesReport(List<SaleRecord> salesRecords, TimeInterval interval, Double exchange) {

        List<SaleRecord> saleRecords = MoreFluentIterable.from(salesRecords)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(SaleRecord::getDate))
                .toList();

        List<PrintableSale> printableSalesList = new ArrayList<>();
        double sellDollarValue = 0;
        double profitDolarValue = 0;
        double recentProfitDolarValue = 0;
        int totalAmount = 0;

        for (SaleRecord sale : saleRecords) {
            double partialsellDolarValue = sale.getQuantity() * sale.getSalePrice();
            double buyPriceDolarValue = sale.getQuantity() * sale.getPurchaseFmv();

            double partialProfitValue = partialsellDolarValue - buyPriceDolarValue;
            double partialRecentProfitValue = sale.isTaxable() ? partialProfitValue : 0;

            sellDollarValue += partialsellDolarValue;
            profitDolarValue += partialProfitValue;
            recentProfitDolarValue += partialRecentProfitValue;
            totalAmount += sale.getQuantity();

            printableSalesList.add(
                    new PrintableSale(
                            DATE_FORMATTERTER.print(sale.getDate()),
                            DATE_FORMATTERTER.print(sale.getPurchaseDate()),
                            sale.getQuantity(),

                            formatDouble(sale.getPurchaseFmv()),
                            formatDouble(sale.getSalePrice()),
                            formatDouble(sale.getSalePrice() - sale.getPurchaseFmv()),

                            formatDouble(partialsellDolarValue),
                            formatDouble(partialProfitValue),
                            formatDouble(partialsellDolarValue * exchange),
                            formatDouble(partialRecentProfitValue*exchange)
                    )
            );
        }

        String profitForTax;
        if (sellDollarValue * exchange < 100000) {
            // no need to pay taxes
            profitForTax = "";
        } else {
            //pay taxes only from items bought in last 3 years
            profitForTax=formatDouble(recentProfitDolarValue * exchange);
        }

        return map(
                "salesList", printableSalesList,
                "sellCroneValue", formatDouble(sellDollarValue * exchange),
                "sellDollarValue", formatDouble(sellDollarValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "profitRecentCroneValue", formatDouble(recentProfitDolarValue * exchange),
                "exchange", exchange,
                "totalAmount", totalAmount,
                "profitForTax", profitForTax
        );

    }
}
