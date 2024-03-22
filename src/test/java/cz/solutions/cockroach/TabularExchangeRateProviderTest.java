package cz.solutions.cockroach;

import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TabularExchangeRateProviderTest {

    @Test
    void canParseHardcoded() throws Exception {
        TabularExchangeRateProvider rateProvider = TabularExchangeRateProvider.hardcoded();

        assertThat(rateProvider.rateAt(LocalDate.parse("2021-05-16")), is(21.024));
        assertThat(rateProvider.rateAt(LocalDate.parse("2021-05-14")), is(21.024));
    }
}