package cz.solutions.cockroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class RsuReport {
    List<PrintableRsu> printableRsuList;
    double rsuCroneValue;
    double rsuDollarValue;
    double taxableRsuCroneValue;
    int totalAmount;

    public Map<String, ?> asMap() {
        return Map.of(
                "rsuList", printableRsuList,
                "rsuCroneValue", FormatingHelper.formatDouble(rsuCroneValue),
                "rsuDolarValue", FormatingHelper.formatDouble(rsuDollarValue),
                "totalAmount", totalAmount,
                "taxableRsuCroneValue", FormatingHelper.formatDouble(taxableRsuCroneValue)
        );
    }
}
