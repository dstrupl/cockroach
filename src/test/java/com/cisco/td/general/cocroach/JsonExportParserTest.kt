package com.cisco.td.general.cocroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test

class JsonExportParserTest {
    @Test
    fun parses() {
        val actual = JsonExportParser().parse(
            ByteSources.fromFile(TestResource.resourceAsFile("json_export.json"))
        )

        assertThat(actual).isEqualTo(
            ParsedExport(
                listOf(
                    RsuRecord(
                        LocalDate(2023,12,13),
                        2,
                        48.38,
                        LocalDate(2023,12,10),
                    )
                ),
                listOf(
                    EsppRecord(
                        LocalDate(2023,12,23),
                        40,
                        36.21,
                        42.60,
                        50.52,
                        LocalDate(2023,12,20),
                    )
                ),
                listOf(DividendRecord(
                    LocalDate(2023,10,25),
                    84.38
                )),
                listOf(
                    TaxRecord(
                        LocalDate(2023,10,25),
                        -12.66
                )),
                listOf(
                    TaxReversalRecord(
                        LocalDate(2023,2,7),
                        1.88
                )),
                listOf(
                    SaleRecord(
                        LocalDate(2023,9,27),
                        "RS",
                        30,
                        47.62,
                        43.91,
                        43.91,
                        LocalDate(2022,11,10),
                    ),
                    SaleRecord(
                        LocalDate(2023,1,23),
                        "ESPP",
                        15,
                        58.62,
                        37.366,
                        53.00,
                        LocalDate(2023,1,10),
                    )
                ),
                listOf(
                    JournalRecord(
                        LocalDate(2023,2,12),
                        -667873.89,
                        "Journal To Account ...889"
                    )
                )
            )
        )

    }
}