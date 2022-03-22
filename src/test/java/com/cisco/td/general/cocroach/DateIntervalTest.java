package com.cisco.td.general.cocroach;

import org.joda.time.LocalDate;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DateIntervalTest {
    @Test
    public void containsCorrectDates() throws Exception {
        DateInterval interval = DateInterval.year(2021);

        assertThat(interval.contains(new LocalDate(2020, 12, 31)), is(false));
        assertThat(interval.contains(new LocalDate(2021, 1, 1)), is(true));
        assertThat(interval.contains(new LocalDate(2021, 6, 14)), is(true));
        assertThat(interval.contains(new LocalDate(2021, 12, 31)), is(true));
        assertThat(interval.contains(new LocalDate(2022, 1, 1)), is(false));
    }
}