package cz.solutions.cockroach;

import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Arrays;
import java.util.Map;

public class ExchangeRatesReader {

    public TabularExchangeRateProvider parse(String ... files) {

        Map<LocalDate, Double> mapping = Arrays.stream(files)
                .map(this::parseOne)
                .reduce((firstMap, secondMap) -> {
                    firstMap.putAll(secondMap);
                    return firstMap;
                }).orElse(Map.of());

        return new TabularExchangeRateProvider(mapping);

    }

    public Map<LocalDate, Double> parseOne(String data) {
        String header = data.lines().findFirst().orElseThrow();
        String[] headerParts = header.split("\\|");
        int usdIndex = Arrays.asList(headerParts).indexOf("1 USD");
        return data.lines()
                .skip(1)
                .map(line -> parseLine(line, usdIndex))
                .collect(Collectors.toMap(Line::getDate, Line::getAmount));
    }

    private Line parseLine(String line, int usdIndex) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.YYYY");
        String[] parts = line.split("\\|");
        return new Line(LocalDate.parse(parts[0], formatter), Money.fromString(parts[usdIndex]));
    }

    @Value
    private static class Line {
        @JsonProperty("Datum")
        @JsonFormat(pattern = "dd.MM.YYYY")
        LocalDate date;
        @JsonProperty("1 USD")
        Money rate;

        public double getAmount() {
            return rate.getAmount();
        }
    }

    @Value
    private static class Money {
        double amount;

        public static Money fromString(String input) {
            return new Money(Double.parseDouble(input.replace(',', '.')));
        }
    }
}
