package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class DividendRecord {
    LocalDate date;
    double amount;
}
