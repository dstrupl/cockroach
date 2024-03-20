package com.cisco.td.general.cocroach;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.joda.time.LocalDate;

import java.util.Map;

public class ExchangeRatesReader {
    private final CsvReader csvReader = CsvReader.builder().separator('|').build();

    public TabularExchangeRateProvider parse(ByteSourceChain ... data) {

        Map<LocalDate, Double> mapping = MoreFluentIterable.from(data).map(this::parseOne)
                .collect(CollectionUtils::mergeMapsWithDistinctKeys);

        return new TabularExchangeRateProvider(mapping);

    }

    public Map<LocalDate, Double> parseOne(ByteSourceChain data) {

        return MoreFluentIterable.from(csvReader.readInputContainingHeader(data, Line.class))
                .toFluentMap(
                        Line::getDate,
                        line -> line.getRate().getAmount()
                )
                .immutableCopy();
    }

    @Value
    private static class Line {
        @JsonProperty("Datum")
        @JsonFormat(pattern = "dd.MM.YYYY")
        LocalDate date;
        @JsonProperty("1 USD")
        Money rate;
    }

    @Value
    private static class Money {
        double amount;

        public static Money fromString(String input) {
            return new Money(Double.parseDouble(input.replace(',', '.')));
        }
    }
}
