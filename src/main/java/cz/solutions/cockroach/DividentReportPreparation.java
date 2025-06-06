package cz.solutions.cockroach;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DividentReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public DividendReport generateDividendReport(List<DividendRecord> dividendRecordList, List<TaxRecord> taxRecordList, List<TaxReversalRecord> taxReversalRecordList, DateInterval interval, ExchangeRateProvider exchangeRateProvider) {

        List<DividendRecord> dividendRecords = dividendRecordList.stream()
                .filter(a -> interval.contains(a.getDate()))
                .sorted(Comparator.comparing(DividendRecord::getDate))
                .toList();

        Map<LocalDate, TaxRecord> taxRecords = taxRecordList.stream()
                .filter(a -> interval.contains(a.getDate()))
                .sorted(Comparator.comparing(TaxRecord::getDate))
                .collect(Collectors.toMap(
                        TaxRecord::getDate,
                        value -> value));

        List<TaxReversalRecord> taxReversalRecords = taxReversalRecordList.stream()
                .filter(a -> interval.contains(a.getDate()))
                .sorted(Comparator.comparing(TaxReversalRecord::getDate))
                .toList();


        ArrayList<PrintableDividend> printableDividendList = new ArrayList<>();

        double totalBruttoDollar = 0;
        double totalTaxDollar = 0;
        double totalBruttoCrown = 0;
        double totalTaxCrown = 0;

        for (DividendRecord dividendRecord : dividendRecords) {
            double exchange = exchangeRateProvider.rateAt(dividendRecord.getDate());
            TaxRecord taxRecord = taxRecords.get(dividendRecord.getDate());

            totalBruttoDollar += dividendRecord.getAmount();
            totalTaxDollar += taxRecord.getAmount();
            totalBruttoCrown += dividendRecord.getAmount() * exchange;
            totalTaxCrown += taxRecord.getAmount() * exchange;

            printableDividendList.add(
                    new PrintableDividend(
                            DATE_FORMATTER.print(dividendRecord.getDate()),
                            FormatingHelper.formatDouble(dividendRecord.getAmount()),
                            FormatingHelper.formatExchangeRate(exchange),
                            FormatingHelper.formatDouble(taxRecord.getAmount()),
                            FormatingHelper.formatDouble(exchange * dividendRecord.getAmount()),
                            FormatingHelper.formatDouble(exchange * taxRecord.getAmount())
                    )
            );
        }

        double totalTaxReversalDollar = 0;
        double totalTaxReversalCrown = 0;

        for (TaxReversalRecord taxReversalRecord : taxReversalRecords) {
            totalTaxReversalDollar += taxReversalRecord.getAmount();
            totalTaxReversalCrown += taxReversalRecord.getAmount() * exchangeRateProvider.rateAt(taxReversalRecord.getDate());
        }

        return new DividendReport(
                printableDividendList,
                totalBruttoDollar,
                totalTaxDollar,
                totalBruttoCrown,
                totalTaxCrown,
                totalTaxReversalDollar,
                totalTaxReversalCrown
        );

    }
}
