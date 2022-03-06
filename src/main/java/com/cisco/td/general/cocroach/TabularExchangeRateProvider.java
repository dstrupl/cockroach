package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.io.ByteSources;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;

import java.util.Map;

@RequiredArgsConstructor
public class TabularExchangeRateProvider implements ExchangeRateProvider {
    private final Map<DateTime, Double> knownRates;

    public static TabularExchangeRateProvider hardcoded() throws JsonProcessingException {
        return new ExchangeRatesReader()
                .parse(ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2021.txt"));
    }

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
