package cz.solutions.cockroach;

import lombok.Value;
import org.joda.time.LocalDate;

@Value
public class DateInterval {
    private final LocalDate startInclusive;
    private final LocalDate endExclusive;

    public static DateInterval year(int year) {
        return new DateInterval(new LocalDate(year, 1, 1), new LocalDate(year + 1, 1, 1));
    }

    public boolean contains(LocalDate date) {
        return startInclusive.equals(date) || (startInclusive.isBefore(date) && date.isBefore(endExclusive));
    }
}
