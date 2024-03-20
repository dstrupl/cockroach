package com.cisco.td.general.cocroach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.io.ByteSource;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ExportParser {
    private static final Logger LOGGER = Logger.getLogger(ExportParser.class.getName());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("YYYY/MM/dd").withZoneUTC();
    private static final DateTimeFormatter REVERSE_DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/YYYY").withZoneUTC();

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
    public ParsedExport parse(String data) throws IOException {

        List<RsuRecord> rsuRecords = new ArrayList<>();
        List<EsppRecord> esppRecords = new ArrayList<>();
        List<DividendRecord> dividendRecords = new ArrayList<>();
        List<TaxRecord> taxRecords = new ArrayList<>();
        List<TaxReversalRecord> taxReversalRecords = new ArrayList<>();
        List<JournalRecord> journalRecords = new ArrayList<>();
        List<SaleRecord> saleRecords = new ArrayList<>();

        List<String> lines = data.lines().toList();

        for (int i = 1; i < lines.size(); i++) {

            try {
                String[] values;
                values = objectReader.readValue(lines.get(i));

                if (values.length > 2 && !values[0].isEmpty()) {

                    LocalDate date = REVERSE_DATE_FORMATTER.parseLocalDate(values[0]);

                    String action = values[1];
                    String symbol = values[2];
                    String description = values[3];
                    String quantity = values[4];
                    String amount = values[7];


                    switch (action) {
                        case "Deposit":
                            switch (description) {
                                case "RS":
                                    String[] rsValues = objectReader.readValue(lines.get(i + 1));
                                    rsuRecords.add(
                                            new RsuRecord(
                                                    date,
                                                    Integer.parseInt(quantity),
                                                    parseAmount(rsValues[16]),
                                                    REVERSE_DATE_FORMATTER.parseLocalDate(rsValues[15])
                                            )
                                    );
                                    break;

                                case "ESPP":
                                    String[] esppValues = objectReader.readValue(lines.get(i + 1));
                                    esppRecords.add(
                                            new EsppRecord(
                                                    date,
                                                    Integer.parseInt(quantity),
                                                    parseAmount(esppValues[9]),
                                                    parseAmount(esppValues[11]),
                                                    parseAmount(esppValues[12]),
                                                    REVERSE_DATE_FORMATTER.parseLocalDate(esppValues[8])
                                            )
                                    );
                                    break;

                                case "Div Reinv":
                                    break;

                                default:
                                    throw new IllegalStateException("Unexpected value: " + description);
                            }

                            break;

                        case "Dividend":
                            dividendRecords.add(
                                    new DividendRecord(
                                            date,
                                            parseAmount(amount)
                                    )
                            );
                            break;

                        case "Tax Withholding":
                            taxRecords.add(
                                    new TaxRecord(
                                            date,
                                            parseAmount(amount)
                                    )
                            );
                            break;

                        case "Tax Reversal":
                            taxReversalRecords.add(
                                    new TaxReversalRecord(
                                            date,
                                            parseAmount(amount)
                                    )
                            );
                            break;

                        case "Journal":
                            journalRecords.add(
                                    new JournalRecord(
                                            date,
                                            parseAmount(amount),
                                            description
                                    )
                            );
                            break;

                        case "Sale":
                            String[] salesValues = objectReader.readValue(lines.get(i + 1));
                            switch(salesValues[17]) {
                                case "RS":
                                    saleRecords.add(
                                            new SaleRecord(
                                                    date,
                                                    salesValues[17],
                                                    Integer.parseInt(quantity),
                                                    parseAmount(salesValues[19]),
                                                    parseAmount(salesValues[16]),
                                                    parseAmount(salesValues[16]),
                                                    REVERSE_DATE_FORMATTER.parseLocalDate(salesValues[15])
                                            )
                                    );
                                    break;
                                case "ESPP":
                                    saleRecords.add(
                                            new SaleRecord(
                                                    date,
                                                    salesValues[17],
                                                    Integer.parseInt(quantity),
                                                    parseAmount(salesValues[19]),
                                                    parseAmount(salesValues[9]),
                                                    parseAmount(salesValues[12]),
                                                    REVERSE_DATE_FORMATTER.parseLocalDate(salesValues[8])
                                            )
                                    );
                                    break;
                                default:
                                    LOGGER.warning(MessageFormat.format(
                                            "Unknown Sale type {0}, ignoring", salesValues[17]));
                            }
                            break;

                        default:
                            LOGGER.warning(MessageFormat.format(
                                    "Unknown report type item {0}, ignoring", action));
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

    private double parseAmount(String amount){

        if(amount.startsWith("-")){
            return -parsePositiveAmount(StringUtils.stripStart(amount, "-"));
        }else{
            return parsePositiveAmount(amount);
        }
    }

    private double parsePositiveAmount(String amount){
        String preprocessed =  StringUtils.replace(amount,",","");
        return Double.parseDouble(StringUtils.stripStart(preprocessed, "$"));

    }
}
