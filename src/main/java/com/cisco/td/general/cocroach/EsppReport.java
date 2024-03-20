package com.cisco.td.general.cocroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;

@Value
public class EsppReport {
    List<PrintableEspp> printableEsppList;
    double profitCroneValue;
    double profitDolarValue;
    int totalEsppAmount;

    public Map<String,?> asMap(){
        return map(
                "esppList", printableEsppList,
                "profitCroneValue", formatDouble(profitCroneValue),
                "profitDolarValue", formatDouble(profitDolarValue),
                "totalAmount", totalEsppAmount
        );
    }
}
