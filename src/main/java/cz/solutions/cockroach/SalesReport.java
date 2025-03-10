package cz.solutions.cockroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static cz.solutions.cockroach.FormatingHelper.formatDouble;

@Value
public class SalesReport {
    List<PrintableSale> printableSalesList;
    double sellCroneValue;
    double sellDollarValue;
    double profitDolarValue;
    double recentProfitCroneValue;
    int totalAmount;
    double profitForTax;
    double sellCroneForTax;
    double buyCroneForTax;

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
