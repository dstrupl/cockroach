package cz.solutions.cockroach;

import org.joda.time.LocalDate;

public interface ExchangeRateProvider {
    double rateAt(LocalDate day);
}
