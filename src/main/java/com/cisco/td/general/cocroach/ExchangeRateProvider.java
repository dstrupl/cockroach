package com.cisco.td.general.cocroach;

import org.joda.time.DateTime;

import java.util.Date;

public interface ExchangeRateProvider {
    double rateAt(DateTime day);
}
