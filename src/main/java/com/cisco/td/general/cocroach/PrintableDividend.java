package com.cisco.td.general.cocroach;

import lombok.Value;

@Value
public class PrintableDividend {
    String date;
    String bruttoDollar;
    String taxDollar;
    String bruttoCrown;
    String taxCrown;
}
