package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.io.ByteSources;
import lombok.RequiredArgsConstructor;
import org.joda.time.LocalDate;

import java.util.Map;

@RequiredArgsConstructor
public class TabularExchangeRateProvider implements ExchangeRateProvider {
    private final Map<LocalDate, Double> knownRates;

    public static TabularExchangeRateProvider hardcoded()  {
        return new ExchangeRatesReader()
                .parse(ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2021.txt"));
    }

    @Override
    public double rateAt(LocalDate day) {
        for (int i = 0; i < 10; i++) {
            Double maybeValue = knownRates.get(day.minusDays(i));
            if (maybeValue != null) {
                return maybeValue;
            }
        }

        throw new IllegalArgumentException("can not find rate for " + day);
    }
}
