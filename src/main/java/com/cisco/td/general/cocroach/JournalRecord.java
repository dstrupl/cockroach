package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class JournalRecord {
    LocalDate date;
    double amount;
    String description;
}
