package cz.solutions.cockroach

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

fun main(args: Array<String>) {
    CockroachMain.report(File(args[0]), args[1].toInt(), File(args[2]))
}

object CockroachMain {
    private val LOGGER = Logger.getLogger(CockroachMain::class.java.name)

    fun report(schwabExportFile: File, year: Int, outputDir: File) {
        val parsedExport = parseExportFile(schwabExportFile)
        val fixedRateReport = ReportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded())
        val dynamicRateReport = ReportGenerator.generateForYear(parsedExport, year, TabularExchangeRateProvider.hardcoded())

        reportOneVariant(year, outputDir, fixedRateReport, "fixed")
        reportOneVariant(year, outputDir, dynamicRateReport, "dynamic")

        recommendBetterAlternative(fixedRateReport, dynamicRateReport, true)
        recommendBetterAlternative(fixedRateReport, dynamicRateReport, false)

        println("######################################################")
        println("# Let's see the guide in ${File(outputDir, "<fixed/dynamic>_guide_$year.html").absolutePath}")
        println("######################################################")
    }

    private fun reportOneVariant(year: Int, outputDir: File, data: Report, dollarConversionSchema: String) {
        File(outputDir, "${dollarConversionSchema}_dividend_$year.md").writeText(data.getDividend(), StandardCharsets.UTF_8)
        File(outputDir, "${dollarConversionSchema}_rsu_$year.md").writeText(data.getRsu(), StandardCharsets.UTF_8)
        File(outputDir, "${dollarConversionSchema}_espp_$year.md").writeText(data.getEspp(), StandardCharsets.UTF_8)
        File(outputDir, "${dollarConversionSchema}_sales_$year.md").writeText(data.getSales(), StandardCharsets.UTF_8)
        File(outputDir, "${dollarConversionSchema}_guide_$year.html").writeText(data.getGuide(), StandardCharsets.UTF_8)
    }

    private fun parseExportFile(schwabExportFile: File): ParsedExport {
        return if (schwabExportFile.extension == "json") {
            JsonExportParser().parse(load(schwabExportFile))
        } else {
            throw IllegalArgumentException("only .json files are supported")
        }
    }

    private fun recommendBetterAlternative(fixedRateReport: Report, dynamicRateReport: Report, use2024Legislative: Boolean) {
        val profitWhenUsedFixedRate = fixedRateReport.rsuAndEsppAndSalesProfitCroneValue(use2024Legislative)
        val profitWhenUsedDynamicRate = dynamicRateReport.rsuAndEsppAndSalesProfitCroneValue(use2024Legislative)

        val recommendation = if (profitWhenUsedFixedRate <= profitWhenUsedDynamicRate) {
            "Use fixed Dollar conversion rate, because ${FormatingHelper.formatDouble(profitWhenUsedFixedRate)}<=${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)} (diff=${FormatingHelper.formatDouble(profitWhenUsedDynamicRate - profitWhenUsedFixedRate)})"
        } else {
            "Use dynamic Dollar conversion rate, because ${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)}<${FormatingHelper.formatDouble(profitWhenUsedFixedRate)} (diff=${FormatingHelper.formatDouble(profitWhenUsedFixedRate - profitWhenUsedDynamicRate)})"
        }

        println("######################################################")
        if (use2024Legislative) {
            println("# Recommendation with 2024 legislative: ")
        } else {
            println("# Recommendation with old legislative: ")
        }
        println("# $recommendation")
        println("######################################################")
        println()
    }

    fun load(file: File): String {
        return try {
            file.readText(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Could not load file ${file.absolutePath}", e)
        }
    }
}