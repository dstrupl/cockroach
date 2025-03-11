package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cz.solutions.cockroach.FormatingHelper.formatDouble;
import static cz.solutions.cockroach.FormatingHelper.formatExchangeRate;

public class RsuReportPreparation {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("dd.MM.YYYY").withZoneUTC();

    public RsuReport generateRsuReport(List<RsuRecord> rsuRecordList,
                                       List<SaleRecord> saleRecordList,
                                       DateInterval interval,
                                       ExchangeRateProvider exchangeRateProvider) {
        List<RsuRecord> rsuRecords = rsuRecordList.stream()
                .filter(a -> interval.contains(a.getVestDate()))
                .sorted(Comparator.comparing(RsuRecord::getDate))
                .toList();

        List<SaleRecord> rsuSaleRecords = saleRecordList.stream()
                .filter(a -> interval.contains(a.getDate()))
                .filter(a->a.getType().equals("RS"))
                .sorted(Comparator.comparing(SaleRecord::getDate))
                .toList();


        ArrayList<PrintableRsu> printableRsuList = new ArrayList<>();
        double rsuCroneValue = 0;
        double taxableRsuCroneValue = 0;
        double rsuDolarValue = 0;
        int totalAmount = 0;

        for (RsuRecord rsu : rsuRecords) {

            // Check if it was sold (it is present in rsuSaleRecords)
            // only part may have been sold
            //multiple sell operations may exist

            double soldQuantity = rsuSaleRecords.stream()
                    .filter(it -> rsu.getVestDate().equals(it.getPurchaseDate()))
                    .filter(it -> rsu.getGrantId().equals(it.getGrantId().orElseThrow()))
                    .mapToDouble(SaleRecord::getQuantity)
                    .sum();

            RsuInfo rsuInfo= withConvertedPrices(rsu,soldQuantity,exchangeRateProvider);

            printableRsuList.add(rsuInfo.toPrintable());

            rsuCroneValue += rsuInfo.vestCroneValue;
            rsuDolarValue += rsuInfo.vestDolarValue;
            totalAmount += rsuInfo.amount;
            taxableRsuCroneValue+=rsuInfo.taxableVestCroneValue;

        }


        return new RsuReport(
                printableRsuList,
                rsuCroneValue,
                rsuDolarValue,
                taxableRsuCroneValue,
                totalAmount
        ) ;
    }

    private RsuInfo withConvertedPrices( RsuRecord rsu,double soldAmount, ExchangeRateProvider exchangeRateProvider){

        double exchange = exchangeRateProvider.rateAt(rsu.getVestDate());
        double partialRsuDolarValue = rsu.getQuantity() * rsu.getVestFmv();
        double partialRsuCroneValue = partialRsuDolarValue * exchange;

        double taxableVestCroneValue= soldAmount* rsu.getVestFmv()*exchange;

        return new RsuInfo(
                rsu.getVestDate(),
                rsu.getQuantity(),
                exchange,
                rsu.getVestFmv(),
                partialRsuDolarValue,
                partialRsuCroneValue,
                soldAmount,
                taxableVestCroneValue
        );
    }

    @Value
    private static class RsuInfo {
        LocalDate date;
        int amount;
        double exchange;
        double onePriceDolarValue;
        double vestDolarValue;
        double vestCroneValue;
        double soldAmount;
        double taxableVestCroneValue;

        PrintableRsu toPrintable(){
            return new PrintableRsu(
                    DATE_FORMATTER.print(date),
                    amount,
                    formatExchangeRate(exchange),
                    formatDouble(onePriceDolarValue),
                    formatDouble(vestDolarValue),
                    formatDouble(vestCroneValue),
                    formatDouble(soldAmount),
                    formatDouble(taxableVestCroneValue)

            );
        }

    }

}
