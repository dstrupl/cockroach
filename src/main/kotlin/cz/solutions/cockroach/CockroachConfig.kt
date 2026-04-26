package cz.solutions.cockroach

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CockroachConfig(
    val year: Int,
    val outputDir: String,
    val schwab: String? = null,
    val etrade: String? = null,
    val degiro: List<String> = emptyList()
) {
    companion object {
        fun load(file: File): CockroachConfig {
            return Yaml.default.decodeFromString(serializer(), file.readText())
        }
    }
}
