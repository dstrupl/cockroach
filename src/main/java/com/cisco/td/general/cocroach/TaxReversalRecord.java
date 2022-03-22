package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class TaxReversalRecord {
    LocalDate date;
    double amount;
}
