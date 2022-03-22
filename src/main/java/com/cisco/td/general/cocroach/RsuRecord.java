package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class RsuRecord {
    LocalDate date;
    int quantity;
    double vestFmv;
    LocalDate vestDate;
}
