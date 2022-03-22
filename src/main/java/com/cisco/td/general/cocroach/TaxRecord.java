package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class TaxRecord {
    LocalDate date;
    double amount;
}
