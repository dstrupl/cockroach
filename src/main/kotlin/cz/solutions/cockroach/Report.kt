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
    private val interestReport: InterestReport,

) {
    private val guideTemplate = TemplateEngine(ReportGenerator::class.java, TemplateHelpers::class.java).load("guide.html.hbs")

    fun getRsuPdf(): ByteArray = RsuReportPdfGenerator.generate(rsuReport)

    fun getRsu2024Pdf(): ByteArray = RsuReportPdfGenerator.generate(rsuReport2024, taxableMode = true)

    fun getDividendPdf(): ByteArray = DividendReportPdfGenerator.generate(dividendReport)

    fun getInterestPdf(): ByteArray = InterestReportPdfGenerator.generate(interestReport)

    fun getEsppPdf(): ByteArray = EsppReportPdfGenerator.generate(esppReport)

    fun getEspp2024Pdf(): ByteArray = EsppReportPdfGenerator.generate(esppReport2024, taxableMode = true)

    fun getSalesPdf(): ByteArray = SalesReportPdfGenerator.generate(salesReport)

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
            "dividendCroneValue" to FormatingHelper.formatRounded(dividendReport.totalNonCzkBruttoCrown),
            "dividendPayedTaxCroneValue" to FormatingHelper.formatRounded(-dividendReport.totalNonCzkTaxCrown)
        )
        val interestVars = mapOf(
            "interestCroneValue" to FormatingHelper.formatRounded(interestReport.totalBruttoCrown),
            "interestForeignCroneValue" to FormatingHelper.formatRounded(interestReport.foreignCountryTotals.sumOf { it.totalBruttoCrown }),
            "interestForeignTaxCroneValue" to FormatingHelper.formatRounded(interestReport.foreignCountryTotals.sumOf { it.totalTaxCrown }),
            "interestCountryTotals" to interestReport.foreignCountryTotals,
            "interestAllCountryTotals" to interestReport.countryTotals,
        )

        val variables = rsuAndEsppVars + salesVars + dividentVars + interestVars

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