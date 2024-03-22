package cz.solutions.cockroach;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TemplateHelpers {
    private static final Map<Long, String> DAYS_LOOKUP =
            IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toMap(Long::valueOf, TemplateHelpers::getOrdinal));
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("eeee, LLLL d")
            .appendText(ChronoField.DAY_OF_MONTH, DAYS_LOOKUP)
            .toFormatter()
            .withZone(ZoneOffset.UTC);

//    public static String durationRound(Duration input) {
//        return DurationUtils.roundDurationToLargest(input);
//    }
    
    public static String upperCase(Object input) {
        return input.toString().toUpperCase();
    }

    public static String lowerCase(Object input) {
        return input.toString().toLowerCase();
    }

    public static String formatDefaultDate(Instant instant) {
        return DEFAULT_DATE_FORMATTER.format(instant);
    }

    // https://stackoverflow.com/a/8474455
    private static String getOrdinal(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
