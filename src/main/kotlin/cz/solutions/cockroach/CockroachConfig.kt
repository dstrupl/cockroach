package cz.solutions.cockroach

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

@Serializable
data class CockroachConfig(
    val year: Int,
    val outputDir: String,
    val schwab: String? = null,
    val etrade: String? = null,
    val etradeBenefitHistory: String? = null,
    val degiro: List<String> = emptyList(),
    val revolut: RevolutConfig = RevolutConfig(),
    val etoro: List<String> = emptyList(),
    val vub: List<String> = emptyList()
) {
    companion object {
        fun load(file: File): CockroachConfig {
            return Yaml.default.decodeFromString(serializer(), file.readText())
        }
    }
}

@Serializable
data class RevolutConfig(
    val whtRate: Double = RevolutParser.DEFAULT_WHT_RATE,
    val stocks: List<String> = emptyList(),
    val savings: List<String> = emptyList()
)
