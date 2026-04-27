package cz.solutions.cockroach

import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        runCockroach(args)
    } catch (e: IllegalArgumentException) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

private fun runCockroach(args: Array<String>) {
    if (args.size == 1 && (args[0].endsWith(".yaml") || args[0].endsWith(".yml"))) {
        val config = CockroachConfig.load(File(args[0]))
        val sources = buildList {
            config.schwab?.let { add(SchwabBrokerSource(File(it))) }
            if (config.etrade != null || config.etradeBenefitHistory != null) {
                add(ETradeBrokerSource(
                    directory = config.etrade?.let { File(it) },
                    benefitHistoryFile = config.etradeBenefitHistory?.let { File(it) },
                ))
            }
            if (config.degiro.isNotEmpty()) add(DegiroBrokerSource(config.degiro.map { File(it) }))
            if (config.revolut.stocks.isNotEmpty() || config.revolut.savings.isNotEmpty()) {
                add(RevolutBrokerSource(
                    stocksFiles = config.revolut.stocks.map { File(it) },
                    savingsFiles = config.revolut.savings.map { File(it) },
                    whtRate = config.revolut.whtRate,
                ))
            }
            if (config.etoro.isNotEmpty()) add(EtoroBrokerSource(config.etoro.map { File(it) }))
            if (config.vub.isNotEmpty()) add(VubBrokerSource(config.vub.map { File(it) }, year = config.year))
        }
        CockroachMain.report(config.year, File(config.outputDir), sources)
        return
    }
    if (args.size < 3) {
        printUsage()
        exitProcess(1)
    }
    val sources = buildList {
        add(SchwabBrokerSource(File(args[0])))
        if (args.size > 3) add(ETradeBrokerSource(directory = File(args[3])))
    }
    CockroachMain.report(args[1].toInt(), File(args[2]), sources)
}

private fun printUsage() {
    System.err.println("Usage: cockroach <config.yaml>                                              (recommended)")
    System.err.println("       cockroach <schwab-json-export> <year> <output-dir> [etrade-dir]     (Schwab/E-Trade only)")
    System.err.println()
    System.err.println("Positional CLI form (limited to E-Trade and Schwab):")
    System.err.println("  schwab-json-export  Path to the Schwab JSON export file")
    System.err.println("  year                Tax year (e.g. 2025)")
    System.err.println("  output-dir          Directory for generated reports")
    System.err.println("  etrade-dir          Optional E-Trade data directory with subdirs:")
    System.err.println("                        rsu/        - RSU release confirmation PDFs")
    System.err.println("                        espp/       - ESPP purchase confirmation PDFs")
    System.err.println("                        dividends/  - single dividends XLSX file")
    System.err.println("                        sales/      - single Gain & Loss CSV/XLSX file")
    System.err.println()
    System.err.println("YAML config form (supports every broker):")
    System.err.println("  year:                  Tax year, e.g. 2025")
    System.err.println("  outputDir:             Directory for generated reports")
    System.err.println("  schwab:                Path to a Schwab JSON export (optional)")
    System.err.println("  etrade:                Path to an E-Trade data directory (optional)")
    System.err.println("  etradeBenefitHistory:  Path to an E-Trade benefit-history XLSX (optional)")
    System.err.println("  degiro:                List of Degiro account-statement CSV paths (optional)")
    System.err.println("  revolut.stocks:        List of Revolut stock statement paths (optional)")
    System.err.println("  revolut.savings:       List of Revolut savings statement paths (optional)")
    System.err.println("  revolut.whtRate:       Withholding-tax rate applied to Revolut dividends (optional)")
    System.err.println("  etoro:                 List of eToro XLSX export paths (optional)")
    System.err.println("  vub:                   List of VÚB interest-confirmation PDF paths (optional)")
    System.err.println()
    System.err.println("At least one broker source must be configured.")
}

object CockroachMain {

    fun report(year: Int, outputDir: File, sources: List<BrokerSource>) {
        require(sources.isNotEmpty()) {
            "No input sources provided. Specify at least one of: schwab, etrade, degiro, revolut, etoro, vub."
        }
        val parsedExport = sources
            .map { it.parse() }
            .fold(ParsedExport.empty()) { acc, e -> acc + e }
        val dailyRateProvider = TabularExchangeRateProvider.fromSource(
            ClasspathOrHttpCnbYearRatesSource(HttpCnbYearRatesSource(HttpCnbYearRatesSource.defaultCacheDir())),
            (year - 1)..year
        )
        val fixedRateReport = ReportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded())
        val dynamicRateReport = ReportGenerator.generateForYear(parsedExport, year, dailyRateProvider)

        reportOneVariant(year, outputDir, fixedRateReport, "fixed")
        reportOneVariant(year, outputDir, dynamicRateReport, "dynamic")

        recommendBetterAlternative(fixedRateReport, dynamicRateReport)


        println("######################################################")
        println("# Let's see the guide in ${File(outputDir, "<fixed/dynamic>_guide_$year.html").absolutePath}")
        println("######################################################")
    }

    private fun reportOneVariant(year: Int, outputDir: File, data: Report, dollarConversionSchema: String) {
        File(outputDir, "${dollarConversionSchema}_dividend_$year.pdf").writeBytes(data.getDividendPdf())
        File(outputDir, "${dollarConversionSchema}_interest_$year.pdf").writeBytes(data.getInterestPdf())
        File(outputDir, "${dollarConversionSchema}_rsu_$year.pdf").writeBytes(data.getRsuPdf())
        File(outputDir, "${dollarConversionSchema}_espp_$year.pdf").writeBytes(data.getEsppPdf())
        File(outputDir, "${dollarConversionSchema}_sales_$year.pdf").writeBytes(data.getSalesPdf())
        File(outputDir, "${dollarConversionSchema}_guide_$year.html").writeText(data.getGuide(), StandardCharsets.UTF_8)

        val transitionYear = ReportGenerator.LEGISLATIVE_TRANSITION_YEAR
        File(outputDir, "${dollarConversionSchema}_rsu_$transitionYear.pdf").writeBytes(data.getRsu2024Pdf())
        File(outputDir, "${dollarConversionSchema}_espp_$transitionYear.pdf").writeBytes(data.getEspp2024Pdf())
    }



    private fun recommendBetterAlternative(fixedRateReport: Report, dynamicRateReport: Report) {
        val profitWhenUsedFixedRate = fixedRateReport.rsuAndEsppAndSalesProfitCroneValue()
        val profitWhenUsedDynamicRate = dynamicRateReport.rsuAndEsppAndSalesProfitCroneValue()

        val profit2024WhenUsedFixedRate = fixedRateReport.rsuAndEsppProfitCroneValue2024()
        val profit2024WhenUsedDynamicRate = dynamicRateReport.rsuAndEsppProfitCroneValue2024()

        val recommendationOldLegislativeUsedIn2024 = if (profitWhenUsedFixedRate <= profitWhenUsedDynamicRate) {
            "Use fixed Dollar conversion rate, because ${FormatingHelper.formatDouble(profitWhenUsedFixedRate)}<=${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)} (diff=${FormatingHelper.formatDouble(profitWhenUsedDynamicRate - profitWhenUsedFixedRate)})"
        } else {
            "Use dynamic Dollar conversion rate, because ${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)}<${FormatingHelper.formatDouble(profitWhenUsedFixedRate)} (diff=${FormatingHelper.formatDouble(profitWhenUsedFixedRate - profitWhenUsedDynamicRate)})"
        }

        val transitionYear = ReportGenerator.LEGISLATIVE_TRANSITION_YEAR
        println("######################################################")
        println("# Recommendation (If old legislative was used for $transitionYear): ")
        println("# $recommendationOldLegislativeUsedIn2024")
        println("######################################################")
        println()



        val recomendationNewLegislativeUsed2024 = if (profit2024WhenUsedFixedRate+profitWhenUsedFixedRate <= profit2024WhenUsedDynamicRate+profitWhenUsedDynamicRate) {
            "Use fixed Dollar conversion rate  because " +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate)}+${FormatingHelper.formatDouble(profitWhenUsedFixedRate)}<=" +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate)}+${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)} " +
                    "(diff=${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate+profitWhenUsedDynamicRate - profit2024WhenUsedFixedRate-profitWhenUsedFixedRate)})"
        } else {
            "Use dynamic Dollar conversion rate, because " +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate)}+${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)}" +
                    "<${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate)} +${FormatingHelper.formatDouble(profitWhenUsedFixedRate)} " +
                    "(diff=${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate +profitWhenUsedFixedRate  - profit2024WhenUsedDynamicRate-profitWhenUsedDynamicRate)})"
        }

        println("######################################################")
        println("# Recommendation (If new legislative was used in $transitionYear) ")
        println("# $recomendationNewLegislativeUsed2024")
        println("######################################################")

    }
}