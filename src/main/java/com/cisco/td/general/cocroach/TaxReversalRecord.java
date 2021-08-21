package com.cisco.td.general.cocroach;

import lombok.Value;
import org.joda.time.DateTime;

@Value
public class TaxReversalRecord {
    DateTime date;
    double amount;
}
