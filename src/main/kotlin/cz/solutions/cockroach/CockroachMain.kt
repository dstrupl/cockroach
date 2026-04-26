package cz.solutions.cockroach

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.logging.Logger
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
        CockroachMain.report(
            schwabExportFile = config.schwab?.let { File(it) },
            year = config.year,
            outputDir = File(config.outputDir),
            eTradeDir = config.etrade?.let { File(it) },
            eTradeBenefitHistoryFile = config.etradeBenefitHistory?.let { File(it) },
            degiroFiles = config.degiro.map { File(it) },
            revolutStocksFiles = config.revolut.stocks.map { File(it) },
            revolutSavingsFiles = config.revolut.savings.map { File(it) },
            revolutWhtRate = config.revolut.whtRate,
            etoroFiles = config.etoro.map { File(it) },
            vubFiles = config.vub.map { File(it) },
        )
        return
    }
    if (args.size < 3) {
        printUsage()
        exitProcess(1)
    }
    val eTradeDir = if (args.size > 3) File(args[3]) else null
    CockroachMain.report(File(args[0]), args[1].toInt(), File(args[2]), eTradeDir)
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

    fun report(
        schwabExportFile: File?,
        year: Int,
        outputDir: File,
        eTradeDir: File? = null,
        eTradeBenefitHistoryFile: File? = null,
        degiroFiles: List<File> = emptyList(),
        revolutStocksFiles: List<File> = emptyList(),
        revolutSavingsFiles: List<File> = emptyList(),
        revolutWhtRate: Double = RevolutParser.DEFAULT_WHT_RATE,
        etoroFiles: List<File> = emptyList(),
        vubFiles: List<File> = emptyList()
    ) {
        val schwabExport = schwabExportFile?.let { parseExportFile(it) } ?: ParsedExport.empty()
        val eTradeExport = if (eTradeDir != null || eTradeBenefitHistoryFile != null) {
            parseETradeDir(eTradeDir, eTradeBenefitHistoryFile)
        } else ParsedExport.empty()
        val degiroExport = degiroFiles.map { parseDegiroFile(it) }.fold(ParsedExport.empty()) { acc, e -> acc + e }
        val revolutStocksExport = revolutStocksFiles.map { parseRevolutStocksFile(it, revolutWhtRate) }
            .fold(ParsedExport.empty()) { acc, e -> acc + e }
        val revolutSavingsExport = revolutSavingsFiles.map { parseRevolutSavingsFile(it) }
            .fold(ParsedExport.empty()) { acc, e -> acc + e }
        val etoroExport = etoroFiles.map { parseEtoroFile(it) }.fold(ParsedExport.empty()) { acc, e -> acc + e }
        val vubExport = vubFiles.map { parseVubFile(it, year) }.fold(ParsedExport.empty()) { acc, e -> acc + e }
        val parsedExport = schwabExport + eTradeExport + degiroExport +
            revolutStocksExport + revolutSavingsExport + etoroExport + vubExport
        require(parsedExport != ParsedExport.empty()) {
            "No input sources provided. Specify at least one of: schwab, etrade, degiro, revolut, etoro, vub."
        }
        val dailyRateProvider = TabularExchangeRateProvider.fromSource(
            HttpCnbYearRatesSource(HttpCnbYearRatesSource.defaultCacheDir()),
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



    private fun parseExportFile(schwabExportFile: File): ParsedExport {
        return if (schwabExportFile.extension == "json") {
            JsonExportParser().parse(load(schwabExportFile))
        } else {
            throw IllegalArgumentException("only .json files are supported")
        }
    }

    private fun parseDegiroFile(file: File): ParsedExport {
        val result = DegiroAccountStatementParser.parse(file)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = result.dividendRecords,
            taxRecords = result.taxRecords,
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun parseRevolutStocksFile(file: File, whtRate: Double): ParsedExport {
        val result = RevolutParser.parseStocks(file, whtRate)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = result.dividendRecords,
            taxRecords = result.taxRecords,
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun parseRevolutSavingsFile(file: File): ParsedExport {
        val result = RevolutParser.parseSavings(file)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = emptyList(),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList(),
            interestRecords = result.interestRecords
        )
    }

    private fun parseEtoroFile(file: File): ParsedExport {
        val result = EtoroXlsxParser.parse(file)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = result.dividendRecords,
            taxRecords = result.taxRecords,
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun parseVubFile(file: File, year: Int): ParsedExport {
        val interestRecords = VubInterestPdfParser.parse(file, year)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = emptyList(),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList(),
            interestRecords = interestRecords
        )
    }

    private fun parseETradeDir(eTradeDir: File?, benefitHistoryFile: File? = null): ParsedExport {
        val benefitHistory = benefitHistoryFile?.let { ETradeBenefitHistoryParser.parse(it) }
        val rsuRecords = benefitHistory?.rsuRecords
            ?: eTradeDir?.let { RsuPdfParser.parseDirectory(File(it, "rsu")) }
            ?: emptyList()
        val esppRecords = benefitHistory?.esppRecords
            ?: eTradeDir?.let { EsppPdfParser.parseDirectory(File(it, "espp")) }
            ?: emptyList()
        val dividentXlsFile = eTradeDir?.let { locateSingleFile(File(it, "dividends"), "xlsx") }
        val dividendXlsxResult = dividentXlsFile?.let {  DividendXlsxParser.parse(it)}
        val eTradeXlsFile = eTradeDir?.let { locateSingleFile(File(it, "sales"), "xlsx") }
        val eTradeCsvFile = eTradeDir?.let { locateSingleFile(File(it, "sales"), "csv") }

        return ParsedExport(
            rsuRecords = rsuRecords,
            esppRecords = esppRecords,
            saleRecords = eTradeXlsFile?.let { ETradeGainLossXlsParser.parse(it)}
                ?:eTradeCsvFile?.let { ETradeGainLossParser.parse(load(it))}
                ?: emptyList(),
            dividendRecords = dividendXlsxResult?.dividendRecords?: emptyList(),
            taxRecords = dividendXlsxResult?.taxRecords?:emptyList(),
            taxReversalRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun locateSingleFile(directory: File, extension: String): File? {
        if (!directory.exists()){
            return null
        }
        require(directory.isDirectory) { "${directory.absolutePath} is not a directory" }
        val files = directory.listFiles { file -> !file.isHidden  && !file.name.startsWith("~")  && !file.name.startsWith(".") && file.extension.equals(extension, ignoreCase = true) }
            ?.toList() ?: emptyList()
        require(files.size <= 1) {
            if (files.isEmpty()) "No .$extension file found in ${directory.absolutePath}"
            else "Expected max one .$extension file in ${directory.absolutePath}, but found ${files.size}: ${files.map { it.name }}"
        }
        return files.firstOrNull()
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

    fun load(file: File): String {
        return try {
            file.readText(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Could not load file ${file.absolutePath}", e)
        }
    }
}