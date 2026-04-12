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
    private val salesReport: SalesReport,
    private val esppReport2024: EsppReport,
    private val rsuReport2024: RsuReport,

) {
    private val rsuTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("rsu.hbs")
    private val rsuTemplate2024 = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("rsu_2024.hbs")

    private val dividendTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("dividend.hbs")
    private val esppTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("espp.hbs")
    private val espp2024Template = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("espp_2024.hbs")

    private val guideTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("guide.html.hbs")

    fun getRsu(): String {
        return render(rsuTemplate, rsuReport.asMap())
    }

    fun getRsu2024(): String {
        return render(rsuTemplate2024, rsuReport2024.asMap())
    }

    fun getDividend(): String {
        return render(dividendTemplate, dividendReport.asMap())
    }

    fun getEspp(): String {
        return render(esppTemplate, esppReport.asMap())
    }

    fun getEspp2024(): String {
        return render(espp2024Template, esppReport2024.asMap())
    }

    fun getSalesPdf(): ByteArray {
        return SalesReportPdfGenerator.generate(salesReport)
    }

    fun getGuide(): String {
        val rsuAndEsppVars = mapOf(
            "rsuProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.rsuCroneValue),
            "esppProfitCroneValue" to FormatingHelper.formatRounded(esppReport.profitCroneValue),
            "rsuAndEsppProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.rsuCroneValue + esppReport.profitCroneValue),
            "rsuTaxableProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.taxableRsuCroneValue),
            "esppTaxableProfitCroneValue" to FormatingHelper.formatRounded(esppReport.taxableProfitCroneValue),
            "rsuAndEsppTaxableProfitCroneValue" to FormatingHelper.formatRounded(rsuReport.taxableRsuCroneValue + esppReport.taxableProfitCroneValue),

            "rsuTaxableProfitCroneValue2024" to FormatingHelper.formatRounded(rsuReport2024.taxableRsuCroneValue),
            "esppTaxableProfitCroneValue2024" to FormatingHelper.formatRounded(esppReport2024.taxableProfitCroneValue),
            "rsuAndEsppTaxableProfitCroneValue2024" to FormatingHelper.formatRounded(rsuReport2024.taxableRsuCroneValue + esppReport2024.taxableProfitCroneValue)
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

    fun rsuAndEsppAndSalesProfitCroneValue(): Double {
        return  rsuReport.taxableRsuCroneValue + salesReport.profitForTax + esppReport.taxableProfitCroneValue
    }

    fun rsuAndEsppProfitCroneValue2024(): Double {
        return  rsuReport2024.taxableRsuCroneValue+ esppReport2024.taxableProfitCroneValue
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