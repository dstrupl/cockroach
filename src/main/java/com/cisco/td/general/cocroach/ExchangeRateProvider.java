package com.cisco.td.general.cocroach;

import org.joda.time.LocalDate;

public interface ExchangeRateProvider {
    double rateAt(LocalDate day);
}
