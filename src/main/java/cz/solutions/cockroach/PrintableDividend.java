package cz.solutions.cockroach;

import lombok.Value;

@Value
public class PrintableDividend {
    String date;
    String bruttoDollar;
    String exchange;
    String taxDollar;
    String bruttoCrown;
    String taxCrown;
}
