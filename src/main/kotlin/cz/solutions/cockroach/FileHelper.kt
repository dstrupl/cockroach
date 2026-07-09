package cz.solutions.cockroach

import java.io.File
import java.nio.charset.StandardCharsets


fun loadText(file: File): String {
    return try {
        file.readText(StandardCharsets.UTF_8)
    } catch (e: Exception) {
        throw RuntimeException("Could not load file ${file.absolutePath}", e)
    }
}
