package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.time.TimeInterval;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;
import static com.cognitivesecurity.commons.util.Literals.map;

public class DividentReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTERTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();


    public Map<String,?> generateDividendReport(List<DividendRecord> dividendRecordList, List<TaxRecord> taxRecordList, List<TaxReversalRecord> taxReversalRecordList, TimeInterval interval, Double exchange) {

        List<DividendRecord> dividendRecords = MoreFluentIterable.from(dividendRecordList)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(DividendRecord::getDate))
                .toList();

        Map<DateTime, TaxRecord> taxRecords = MoreFluentIterable.from(taxRecordList)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(TaxRecord::getDate))
                .fluentUniqueIndex(TaxRecord::getDate)
                .immutableCopy();

        List<TaxReversalRecord> taxReversalRecords = MoreFluentIterable.from(taxReversalRecordList)
                .filter(a -> interval.includes(a.getDate().getMillis()))
                .sorted(Comparator.comparing(TaxReversalRecord::getDate))
                .toList();


        ArrayList<PrintableDividend> printableDividendList = new ArrayList<>();

        double totalBruttoDollar = 0;
        double totalTaxDollar = 0;
        double totalBruttoCrown = 0;
        double totalTaxCrown = 0;

        for (DividendRecord dividendRecord : dividendRecords) {
            TaxRecord taxRecord = taxRecords.get(dividendRecord.getDate());

            totalBruttoDollar += dividendRecord.getAmount();
            totalTaxDollar += taxRecord.getAmount();
            totalBruttoCrown += dividendRecord.getAmount() * exchange;
            totalTaxCrown += taxRecord.getAmount() * exchange;

            printableDividendList.add(
                    new PrintableDividend(
                            DATE_FORMATTERTER.print(dividendRecord.getDate()),
                            formatDouble(dividendRecord.getAmount()),
                            formatDouble(taxRecord.getAmount()),
                            formatDouble(exchange * dividendRecord.getAmount()),
                            formatDouble(exchange * taxRecord.getAmount())
                    )
            );
        }

        double totalTaxReversalDollar = 0;
        double totalTaxReversalCrown = 0;

        for (TaxReversalRecord taxReversalRecord : taxReversalRecords) {
            totalTaxReversalDollar += taxReversalRecord.getAmount();
            totalTaxReversalCrown += taxReversalRecord.getAmount() * exchange;
        }

        return map(
                "dividendList", printableDividendList,
                "totalBruttoDollar", formatDouble(totalBruttoDollar),
                "totalTaxDollar", formatDouble(totalTaxDollar),
                "exchange", exchange,
                "totalBruttoCrown", formatDouble(totalBruttoCrown),
                "totalTaxCrown", formatDouble(totalTaxCrown),
                "totalTaxReversal", totalTaxReversalDollar > 0 ? formatDouble(totalTaxReversalDollar) : "",
                "totalTaxReversalCrown", totalTaxReversalCrown > 0 ? formatDouble(totalTaxReversalCrown) : ""
        );

    }
}
