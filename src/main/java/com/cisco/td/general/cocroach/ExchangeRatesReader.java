package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.io.ByteSourceChain;
import com.cognitivesecurity.commons.util.Literals;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;

import static com.cognitivesecurity.commons.util.Literals.mapEntry;

public class ExchangeRatesReader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();


    private final ObjectReader objectReader;

    public ExchangeRatesReader() {
        CsvMapper csvMapper = new CsvMapper();

        CsvSchema schema = csvMapper.schemaFor(String[].class)
                .withColumnSeparator('|')
                .withoutQuoteChar()
                .withoutEscapeChar();

        this.objectReader = csvMapper.readerFor(String[].class).with(schema);
    }

    public TabularExchangeRateProvider parse(ByteSourceChain data) throws JsonProcessingException {
        List<String> lines = data.fluentLines().toList();
        String header = lines.get(0);
        Map<String, Integer> itemMap = MoreFluentIterable.from((String[]) objectReader.readValue(header))
                .mapWithIndex((i, item) -> mapEntry(item, i))
                .collect(Literals::mapCopy);

        int datumIndex = itemMap.get("Datum");
        int usdIndex = itemMap.get("1 USD");

        Map<DateTime, Double> ratesTable = MoreFluentIterable.from(lines).skip(1).map(line -> {

            try {
                String[] values;
                values = objectReader.readValue(line);
                return mapEntry(
                        DATE_FORMATTER.parseDateTime(values[datumIndex]),
                        Double.parseDouble(values[usdIndex])
                );

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Literals::mapCopy);


        return new TabularExchangeRateProvider(ratesTable);
    }


}
