package cz.solutions.cockroach;

import lombok.Value;

@Value
public class PrintableSale {
    String date;
    String purchaseDate;
    String amount;
    String exchange;

    String onePurchaseDollar;
    String oneSellDollar;
    String oneProfitDollar;

    String sellDolar;
    String sellProfitDollar;
    String sellCrone;
    String sellRecentProfitCrone;
}
