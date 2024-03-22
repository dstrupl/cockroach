package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class SaleRecord {
    LocalDate date;
    String type;
    int quantity;
    double salePrice;
    double purchasePrice;
    double purchaseFmv;
    LocalDate purchaseDate;

    public boolean isTaxable(){
        return date.isBefore(purchaseDate.plusYears(3));
    }
}
