package com.cisco.td.general.cocroach;

import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;

public class TemplateEngine {
    private final Handlebars handlebars;
    private final Class<?> baseClass;

    public TemplateEngine(Class<?> baseClass) {
        this.baseClass = baseClass;
        this.handlebars = createHandlebars();
    }

    public TemplateEngine(Class<?> baseClass, Class<?> helperSource) {
        this.baseClass = baseClass;
        this.handlebars = createHandlebars()
                .registerHelpers(helperSource);
    }

    private Handlebars createHandlebars() {
        return new Handlebars()
                .with(EscapingStrategy.NOOP)
                .with(NullTemplateCache.INSTANCE)
                .prettyPrint(true);
    }

    public Template load(String templateName) {
        try {
            InputStream is = this.getClass().getResourceAsStream(templateName);
            String templateText = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
            return handlebars.compileInline(templateText);
        } catch (Exception e) {
            throw new RuntimeException("Could not load template " + templateName, e);
        }
    }
}
