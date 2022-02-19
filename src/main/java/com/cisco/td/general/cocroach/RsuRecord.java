package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.DateTime;

@Value
public class RsuRecord {
    DateTime date;
    int quantity;
    double vestFmv;
    DateTime vestDate;
}
