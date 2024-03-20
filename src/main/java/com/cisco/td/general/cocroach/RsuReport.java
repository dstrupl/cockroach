package com.cisco.td.general.cocroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;

@Value
public class RsuReport {
    List<PrintableRsu> printableRsuList;
    double rsuCroneValue;
    double rsuDollarValue;
    int totalAmount;

    public Map<String, ?> asMap() {
        return map(
                "rsuList", printableRsuList,
                "rsuCroneValue", formatDouble(rsuCroneValue),
                "rsuDolarValue", formatDouble(rsuDollarValue),
                "totalAmount", totalAmount
        );
    }
}
