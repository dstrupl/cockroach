package com.cisco.td.general.cocroach;

import lombok.Value;

@Value
public class PrintableRsu {
    String date;
    int amount;
    String onePriceDolarValue;
    String vestDolarValue;
    String vestCroneValue;

}
