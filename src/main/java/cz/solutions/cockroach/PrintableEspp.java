package cz.solutions.cockroach;

import lombok.Value;

@Value
public class PrintableEspp {
    String date;
    int amount;
    String exchange;
    String onePricePurchaseDolarValue;
    String onePriceDolarValue;
    String oneProfitValue;
    String buyProfitValue;
    String buyCronePofitValue;
    int soldAmount;
    String taxableBuyCroneProfitValue;
}
