package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import java.util.*

class JsonExportParserTest {

    private fun loadResource(file: String) = {}::class.java.getResource(file)?.readText()

    @Test
    fun parses() {
        val actual = loadResource("json_export.json")?.let {
            JsonExportParser().parse(it)
        }

        assertThat(actual).isEqualTo(
            ParsedExport(
                listOf(
                    RsuRecord(
                        LocalDate(2023,12,13),
                        2,
                        48.38,
                        LocalDate(2023,12,10),
                        "1461994"
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
                listOf(
                    DividendRecord(
                    LocalDate(2023,10,25),
                    84.38
                )
                ),
                listOf(
                    TaxRecord(
                        LocalDate(2023,10,25),
                        -12.66
                )
                ),
                listOf(
                    TaxReversalRecord(
                        LocalDate(2023,2,7),
                        1.88
                )
                ),
                listOf(
                    SaleRecord(
                        LocalDate(2023,9,27),
                        "RS",
                        30.0,
                        47.62,
                        43.91,
                        43.91,
                        LocalDate(2022,11,10),
                        Optional.of("1538646")
                    ),
                    SaleRecord(
                        LocalDate(2023,1,23),
                        "ESPP",
                        15.0,
                        58.62,
                        37.366,
                        53.00,
                        LocalDate(2023,1,10),
                        Optional.empty()
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