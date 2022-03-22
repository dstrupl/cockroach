package com.cisco.td.general.cocroach;

import com.cisco.td.ade.csv.CsvReader;
import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.io.ByteSourceChain;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.joda.time.LocalDate;

public class ExchangeRatesReader {
    private final CsvReader csvReader = CsvReader.builder().separator('|').build();

    public TabularExchangeRateProvider parse(ByteSourceChain data) {
        return MoreFluentIterable.from(csvReader.readInputContainingHeader(data, Line.class))
                .toFluentMap(
                        Line::getDate,
                        line -> line.getRate().getAmount()
                )
                .convert(TabularExchangeRateProvider::new);
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
        private final double amount;

        public static Money fromString(String input) {
            return new Money(Double.parseDouble(input.replace(',', '.')));
        }
    }
}
