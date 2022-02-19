package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.DateTime;

@Value
public class SaleRecord {
    DateTime date;
    String type;
    int quantity;
    double salePrice;
    double purchasePrice;
    double purchaseFmv;
    DateTime purchaseDate;
}
