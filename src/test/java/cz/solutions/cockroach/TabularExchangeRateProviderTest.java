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
        assertThat(rateProvider.rateAt(LocalDate.parse("2022-05-15")), is(23.825));
        assertThat(rateProvider.rateAt(LocalDate.parse("2023-05-15")), is(21.671));
        assertThat(rateProvider.rateAt(LocalDate.parse("2024-05-15")), is(22.861));
    }
}