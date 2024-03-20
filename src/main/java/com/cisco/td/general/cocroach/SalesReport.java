package com.cisco.td.general.cocroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;

@Value
public class SalesReport {
    List<PrintableSale> printableSalesList;
    double sellCroneValue;
    double sellDollarValue;
    double profitDolarValue;
    double recentProfitCroneValue;
    int totalAmount;
    double profitForTax;

    public Map<String,?> asMap(){
        return Map.of(
                "salesList", printableSalesList,
                "sellCroneValue", formatDouble(sellCroneValue),
                "sellDollarValue", formatDouble(sellDollarValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "profitRecentCroneValue", formatDouble(recentProfitCroneValue),
                "totalAmount", totalAmount,
                "profitForTax",profitForTax>0 ?  formatDouble(profitForTax):""
        );
    }
}
