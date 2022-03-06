package com.cisco.td.general.cocroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static com.cognitivesecurity.commons.util.Literals.map;

@Value
public class RsuReport {
    List<PrintableRsu> printableRsuList;
    double rsuCroneValue;
    double rsuDolarValue;
    int totalAmount;

    public Map<String, ?> asMap() {
        return map(
                "rsuList", printableRsuList,
                "rsuCroneValue", String.format("%.4f", rsuCroneValue),
                "rsuDolarValue", String.format("%.4f", rsuDolarValue),
                "totalAmount", totalAmount
        );
    }
}
