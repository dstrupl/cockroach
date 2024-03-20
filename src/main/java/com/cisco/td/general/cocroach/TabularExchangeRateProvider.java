package com.cisco.td.general.cocroach;

import com.github.jknack.handlebars.Template;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.io.CharStreams;
import org.joda.time.LocalDate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NavigableMap;

public class TabularExchangeRateProvider implements ExchangeRateProvider {
    private final NavigableMap<LocalDate, Double> knownRates;

    public TabularExchangeRateProvider(Map<LocalDate, Double> knownRates) {
        this.knownRates = ImmutableSortedMap.copyOf(knownRates);
    }

    public static TabularExchangeRateProvider hardcoded() {
        return new ExchangeRatesReader().parse(
                load("rates_2021.txt"),
                load("rates_2022_a.txt"),
                load("rates_2022_b.txt"),
                load("rates_2023.txt")
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

    public static String load(String fileName) {
        try {
            InputStream is = TabularExchangeRateProvider.class.getResourceAsStream(fileName);
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Could not load template " + fileName, e);
        }
    }
}
