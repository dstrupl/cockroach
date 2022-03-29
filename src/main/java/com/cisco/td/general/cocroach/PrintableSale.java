package com.cisco.td.general.cocroach;

import lombok.Value;

@Value
public class PrintableSale {
    String date;
    String purchaseDate;
    int amount;
    String exchange;

    String onePurchaseDollar;
    String oneSellDollar;
    String oneProfitDollar;

    String sellDolar;
    String sellProfitDollar;
    String sellCrone;
    String sellRecentProfitCrone;
}
