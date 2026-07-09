package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CockroachConfigTest {

    @Test
    fun loadsInteractiveBrokersAlongsideOtherOptionalSources(@TempDir tempDir: File) {
        val yaml = File(tempDir, "config.yaml")
        yaml.writeText(
            """
            year: 2025
            outputDir: ./output
            schwab: ./input/schwab.json
            ib: ./input/ib
            """.trimIndent()
        )

        val config = CockroachConfig.load(yaml)

        assertThat(config.year).isEqualTo(2025)
        assertThat(config.schwab).isEqualTo("./input/schwab.json")
        assertThat(config.ib).isEqualTo("./input/ib")
    }
}
