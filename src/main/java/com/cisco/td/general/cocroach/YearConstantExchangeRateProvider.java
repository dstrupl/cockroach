package com.cisco.td.general.cocroach;

import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;

import java.util.Map;

import static com.cognitivesecurity.commons.util.Literals.map;

@RequiredArgsConstructor
public class YearConstantExchangeRateProvider implements ExchangeRateProvider {

    private final Map<Integer, Double> exchange;

    public static YearConstantExchangeRateProvider hardcoded() {
        return new YearConstantExchangeRateProvider(
                map(
                        2018, 21.780,
                        2019, 22.930,
                        2020, 23.140,
                        2021, 21.72
                )
        );
    }


    @Override
    public double rateAt(DateTime day) {
        Double maybeResult = exchange.get(day.getYear());
        if (maybeResult == null) {
            throw new IllegalArgumentException("can not find rate for " + day);
        }
        return maybeResult;
    }
}
