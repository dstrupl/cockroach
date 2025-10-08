package cz.solutions.cockroach

import com.github.jknack.handlebars.EscapingStrategy
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.cache.NullTemplateCache
import java.nio.charset.StandardCharsets

class TemplateEngine(private val baseClass: Class<*>) {
    private val handlebars: Handlebars = createHandlebars()

    constructor(baseClass: Class<*>, helperSource: Class<*>) : this(baseClass) {
        handlebars.registerHelpers(helperSource)
    }

    private fun createHandlebars(): Handlebars {
        return Handlebars()
            .with(EscapingStrategy.NOOP)
            .with(NullTemplateCache.INSTANCE)
            .prettyPrint(true)
    }

    fun load(templateName: String): Template {
        return try {
            val templateText = baseClass.getResourceAsStream(templateName)?.use {
                it.reader(StandardCharsets.UTF_8).readText()
            } ?: throw RuntimeException("Could not load template $templateName")
            handlebars.compileInline(templateText)
        } catch (e: Exception) {
            throw RuntimeException("Could not load template $templateName", e)
        }
    }
}