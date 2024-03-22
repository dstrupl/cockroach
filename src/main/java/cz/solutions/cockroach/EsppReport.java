package cz.solutions.cockroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static cz.solutions.cockroach.FormatingHelper.formatDouble;

@Value
public class EsppReport {
    List<PrintableEspp> printableEsppList;
    double profitCroneValue;
    double profitDolarValue;
    int totalEsppAmount;

    public Map<String,?> asMap(){
        return Map.of(
                "esppList", printableEsppList,
                "profitCroneValue", formatDouble(profitCroneValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "totalAmount", totalEsppAmount
        );
    }
}
