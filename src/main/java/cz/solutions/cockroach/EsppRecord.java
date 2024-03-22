package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class EsppRecord {
    LocalDate date;
    int quantity;
    double purchasePrice;
    double subscriptionFmv;
    double purchaseFmv;
    LocalDate purchaseDate;
}
