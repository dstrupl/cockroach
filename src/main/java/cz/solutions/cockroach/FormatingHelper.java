package cz.solutions.cockroach;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import static java.lang.Math.round;

public class FormatingHelper {

    private static final FormatingHelper INSTANCE = new FormatingHelper();

    private final NumberFormat format;

    private FormatingHelper() {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator(',');
        formatSymbols.setGroupingSeparator('.');

        format = new DecimalFormat("#,##0.00", formatSymbols);
    }

    private String format(double d) {
        return format.format(d);
    }

    public static String formatDouble(double d) {
        return INSTANCE.format(d);
    }

    public static String formatRounded(double d) {
        return  String.valueOf(round(d));
    }

    public static String formatExchangeRate(double d) {
        return String.valueOf(d).replace('.', ',');
    }
}
