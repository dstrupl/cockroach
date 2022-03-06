package com.cisco.td.general.cocroach;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TabularExchangeRateProviderTest {

    @Test
    void canParseHardcoded() throws Exception {
        TabularExchangeRateProvider rateProvider = TabularExchangeRateProvider.hardcoded();

        assertThat(rateProvider.rateAt(DateTime.parse("2021-05-16T00:00:00.00Z")),is(21.024));
        assertThat(rateProvider.rateAt(DateTime.parse("2021-05-14T00:00:00.00Z")),is(21.024));
    }
}