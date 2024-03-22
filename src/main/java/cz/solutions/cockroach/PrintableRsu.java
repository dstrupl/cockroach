package cz.solutions.cockroach;

import lombok.Value;

@Value
public class PrintableRsu {
    String date;
    int amount;
    String exchange;
    String onePriceDolarValue;
    String vestDolarValue;
    String vestCroneValue;

}
