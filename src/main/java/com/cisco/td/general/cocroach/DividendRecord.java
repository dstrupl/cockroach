package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.DateTime;

@Value
public class DividendRecord {
    DateTime date;
    double amount;
}
