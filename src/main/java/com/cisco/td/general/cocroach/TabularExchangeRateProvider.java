package com.cisco.td.general.cocroach;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

@RequiredArgsConstructor
public class TabularExchangeRateProvider implements ExchangeRateProvider {
    private final Map<DateTime, Double> knownRates;

    @Override
    public double rateAt(DateTime day) {
        for (int i = 0; i < 10; i++) {
            Double maybeValue = knownRates.get(day.minusDays(i));
            if (maybeValue != null) {
                return maybeValue;
            }
        }

        throw new IllegalArgumentException("can not find rate for " + day);
    }
}
