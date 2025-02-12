package cz.solutions.cockroach;

import lombok.RequiredArgsConstructor;
import org.joda.time.LocalDate;

import java.util.Map;


@RequiredArgsConstructor
public class YearConstantExchangeRateProvider implements ExchangeRateProvider {

    private final Map<Integer, Double> exchange;

    public static YearConstantExchangeRateProvider hardcoded() {
        return new YearConstantExchangeRateProvider(
                Map.of(
                        2018, 21.780,
                        2019, 22.930,
                        2020, 23.140,
                        2021, 21.72,
                        2022, 23.41,
                        2023, 22.14,
                        2024, 23.28
                )
        );
    }

    @Override
    public double rateAt(LocalDate day) {
        Double maybeResult = exchange.get(day.getYear());
        if (maybeResult == null) {
            throw new IllegalArgumentException("can not find rate for " + day);
        }
        return maybeResult;
    }
}
