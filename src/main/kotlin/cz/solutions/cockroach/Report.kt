package cz.solutions.cockroach

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.context.FieldValueResolver
import com.github.jknack.handlebars.context.JavaBeanValueResolver
import com.github.jknack.handlebars.context.MapValueResolver
import java.io.IOException

class Report(
    private val rsuReport: RsuReport,
    private val dividendReport: DividendReport,
    private val esppReport: EsppReport,
    private val salesReport: SalesReport
) {
    private val rsuTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("rsu.hbs")
    private val dividendTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("dividend.hbs")
    private val esppTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("espp.hbs")
    private val salesTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("sales.hbs")
    private val guideTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("guide.html.hbs")

    fun getRsu(): String {
        return render(rsuTemplate, rsuReport.asMap())
    }

    fun getDividend(): String {
        return render(dividendTemplate, dividendReport.asMap())
    }

    fun getEspp(): String {
        return render(esppTemplate, esppReport.asMap())
    }

    fun getSales(): String {
        return render(salesTemplate, salesReport.asMap())
    }

    fun getGuide(): String {
        val rsuAndEsppVars = mapOf(
            "rsuProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.rsuCroneValue),
            "esppProfitCroneValue" to FormatingHelper.formatRounded(esppReport.profitCroneValue),
            "rsuAndEsppProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.rsuCroneValue + esppReport.profitCroneValue),
            "rsuTaxableProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.taxableRsuCroneValue),
            "esppTaxableProfitCroneValue" to FormatingHelper.formatRounded(esppReport.taxableProfitCroneValue),
            "rsuAndEsppTaxableProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.taxableRsuCroneValue + esppReport.taxableProfitCroneValue)
        )
        val salesVars = mapOf(
            "taxableSellPriceCroneValue" to FormatingHelper.formatRounded(salesReport.sellCroneForTax),
            "taxableBuyPriceCroneValue" to FormatingHelper.formatRounded(salesReport.buyCroneForTax),
            "taxableSellProfitCroneValue" to FormatingHelper.formatRounded(salesReport.profitForTax)
        )
        val dividentVars = mapOf(
            "dividendCroneValue" to FormatingHelper.formatRounded(dividendReport.totalBruttoCrown),
            "dividendPayedTaxCroneValue" to FormatingHelper.formatRounded(-dividendReport.totalTaxCrown)
        )

        val variables = rsuAndEsppVars + salesVars + dividentVars

        return render(guideTemplate, variables)
    }

    fun rsuAndEsppAndSalesProfitCroneValue(use2024Legislative: Boolean): Double {
        return if (use2024Legislative) {
            rsuReport.taxableRsuCroneValue + salesReport.profitForTax + esppReport.taxableProfitCroneValue
        } else {
            rsuReport.rsuCroneValue + salesReport.profitForTax + esppReport.profitCroneValue
        }
    }

    private fun render(template: Template, variables: Map<String, Any>): String {
        return try {
            val context = Context.newBuilder(variables)
                .resolver(
                    MapValueResolver.INSTANCE,
                    JavaBeanValueResolver.INSTANCE,
                    FieldValueResolver.INSTANCE
                )
                .build()
            template.apply(context)
        } catch (e: IOException) {
            throw RuntimeException("Could not render template", e)
        }
    }
}