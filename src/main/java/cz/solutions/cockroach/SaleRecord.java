package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;

import java.util.Optional;

@Value
public class SaleRecord {
    LocalDate date;
    String type;
    double quantity;
    double salePrice;
    double purchasePrice;
    double purchaseFmv;
    LocalDate purchaseDate;
    Optional<String> grantId;

    public boolean isTaxable(){
        return date.isBefore(purchaseDate.plusYears(3));
    }
}
