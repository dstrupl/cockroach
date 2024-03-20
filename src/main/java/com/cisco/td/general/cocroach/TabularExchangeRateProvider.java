package com.cisco.td.general.cocroach;

import com.google.common.collect.ImmutableSortedMap;
import org.joda.time.LocalDate;

import java.util.Map;
import java.util.NavigableMap;

public class TabularExchangeRateProvider implements ExchangeRateProvider {
    private final NavigableMap<LocalDate, Double> knownRates;

    public TabularExchangeRateProvider(Map<LocalDate, Double> knownRates) {
        this.knownRates = ImmutableSortedMap.copyOf(knownRates);
    }

    public static TabularExchangeRateProvider hardcoded() {
        return new ExchangeRatesReader().parse(
                ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2021.txt"),
                ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2022_a.txt"),
                ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2022_b.txt"),
                ByteSources.fromResource(TabularExchangeRateProvider.class, "rates_2023.txt")
        );
    }

    @Override
    public double rateAt(LocalDate day) {
        Map.Entry<LocalDate, Double> maybeEntry = knownRates.floorEntry(day);
        if (maybeEntry != null) {
            return maybeEntry.getValue();
        } else {
            throw new IllegalArgumentException("can not find rate for " + day);
        }
    }
}
