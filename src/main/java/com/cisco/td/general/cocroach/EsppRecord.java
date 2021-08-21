package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.DateTime;

@Value
public class EsppRecord {
    DateTime date;
    int quantity;
    double purchasePrice;
    double subscriptionFmv;
    double purchaseFmv;
}
