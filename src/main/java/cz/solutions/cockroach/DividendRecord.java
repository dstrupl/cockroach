package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class DividendRecord {
    LocalDate date;
    double amount;
}
