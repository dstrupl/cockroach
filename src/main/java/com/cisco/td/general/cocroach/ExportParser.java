package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.io.ByteSourceChain;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class ExportParser {

    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("YYYY/MM/dd").withZoneUTC();
    private static final DateTimeFormatter REVERSE_DATE_FORMATTERTER = DateTimeFormat.forPattern("MM/dd/YYYY").withZoneUTC();

    private final ObjectReader objectReader;

    public ExportParser() {
        CsvMapper csvMapper = new CsvMapper();

        CsvSchema schema = csvMapper.schemaFor(String[].class)
                .withColumnSeparator(',')
                .withQuoteChar('"')
                .withoutEscapeChar();

        this.objectReader = csvMapper.readerFor(String[].class).with(schema);
    }

    @SuppressWarnings("NestedSwitchStatement")
    public ParsedExport parse(ByteSourceChain data) {

        List<RsuRecord> rsuRecords = new ArrayList<>();
        List<EsppRecord> esppRecords = new ArrayList<>();
        List<DividendRecord> dividendRecords = new ArrayList<>();
        List<TaxRecord> taxRecords = new ArrayList<>();
        List<TaxReversalRecord> taxReversalRecords = new ArrayList<>();
        List<JournalRecord> journalRecords = new ArrayList<>();
        List<SaleRecord> saleRecords = new ArrayList<>();


        List<String> lines = data.fluentLines().toList();

        for (int i = 2; i < lines.size(); i++) {

            try {
                String[] values;
                values = objectReader.readValue(lines.get(i));

                if (values.length > 2 && !values[0].isEmpty()) {

                    DateTime date = DATE_FORMATTERTER.parseDateTime(values[0]);

                    String action = values[1];
                    String symbol = values[2];
                    String description = values[3];
                    String quantity = values[4];
                    String amount = values[7];


                    switch (action) {
                        case "Deposit":
                            switch (description) {
                                case "RS":
                                    String[] rsValues = objectReader.readValue(lines.get(i + 2));
                                    rsuRecords.add(
                                            new RsuRecord(
                                                    date,
                                                    Integer.parseInt(quantity),
                                                    Double.parseDouble(StringUtils.stripStart(rsValues[4], "$"))
                                            )
                                    );
                                    break;

                                case "ESPP":
                                    String[] esppValues = objectReader.readValue(lines.get(i + 2));
                                    esppRecords.add(
                                            new EsppRecord(
                                                    date,
                                                    Integer.parseInt(quantity),
                                                    Double.parseDouble(StringUtils.stripStart(esppValues[2], "$")),
                                                    Double.parseDouble(StringUtils.stripStart(esppValues[4], "$")),
                                                    Double.parseDouble(StringUtils.stripStart(esppValues[5], "$"))
                                            )
                                    );
                                    break;

                                default:
                                    throw new IllegalStateException("Unexpected value: " + description);
                            }

                            break;

                        case "Dividend":
                            dividendRecords.add(
                                    new DividendRecord(
                                            date,
                                            Double.parseDouble(StringUtils.stripStart(amount, "$"))
                                    )
                            );
                            break;

                        case "Tax Withholding":
                            taxRecords.add(
                                    new TaxRecord(
                                            date,
                                            Double.parseDouble(StringUtils.stripStart(amount, "-$"))
                                    )
                            );
                            break;

                        case "Tax Reversal":
                            taxReversalRecords.add(
                                    new TaxReversalRecord(
                                            date,
                                            Double.parseDouble(StringUtils.stripStart(amount, "$"))
                                    )
                            );
                            break;

                        case "Journal":
                            journalRecords.add(
                                    new JournalRecord(
                                            date,
                                            Double.parseDouble(StringUtils.stripStart(amount, "$")),
                                            description
                                    )
                            );
                            break;

                        case "Sale":
                            String[] salesValues = objectReader.readValue(lines.get(i + 2));
                            saleRecords.add(
                                    new SaleRecord(
                                            date,
                                            salesValues[1],
                                            Integer.parseInt(quantity),
                                            Double.parseDouble(StringUtils.stripStart(salesValues[3], "$")),
                                            Double.parseDouble(StringUtils.stripStart(salesValues[7], "$")),
                                            Double.parseDouble(StringUtils.stripStart(salesValues[8], "$")),
                                            REVERSE_DATE_FORMATTERTER.parseDateTime(salesValues[6])
                                    )
                            );
                            break;

                        default:
                            throw new IllegalStateException("Unexpected value: " + action);
                    }
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }

        return new ParsedExport(
                rsuRecords,
                esppRecords,
                dividendRecords,
                taxRecords,
                taxReversalRecords,
                saleRecords,
                journalRecords
        );
    }
}
