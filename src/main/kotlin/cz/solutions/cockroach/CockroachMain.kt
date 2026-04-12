package cz.solutions.cockroach

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

fun main(args: Array<String>) {
    if (args.size < 3) {
        System.err.println("Usage: cockroach <schwab-json-export> <year> <output-dir> [etrade-dir]")
        System.err.println()
        System.err.println("  schwab-json-export  Path to the Schwab JSON export file")
        System.err.println("  year                Tax year (e.g. 2025)")
        System.err.println("  output-dir          Directory for generated reports")
        System.err.println("  etrade-dir          Optional E-Trade data directory with subdirs:")
        System.err.println("                        rsu/        - RSU release confirmation PDFs")
        System.err.println("                        espp/       - ESPP purchase confirmation PDFs")
        System.err.println("                        dividends/  - single dividends XLSX file")
        System.err.println("                        sales/      - single Gain & Loss CSV file")
        System.exit(1)
    }
    val eTradeDir = if (args.size > 3) File(args[3]) else null
    CockroachMain.report(File(args[0]), args[1].toInt(), File(args[2]), eTradeDir)
}

object CockroachMain {
    private val LOGGER = Logger.getLogger(CockroachMain::class.java.name)

    fun report(schwabExportFile: File, year: Int, outputDir: File, eTradeDir: File? = null) {
        val schwabExport = parseExportFile(schwabExportFile)
        val eTradeExport = eTradeDir?.let { parseETradeDir(it) } ?: ParsedExport.empty()
        val parsedExport = schwabExport + eTradeExport
        val fixedRateReport = ReportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded())
        val dynamicRateReport = ReportGenerator.generateForYear(parsedExport, year, TabularExchangeRateProvider.hardcoded())

        reportOneVariant(year, outputDir, fixedRateReport, "fixed")
        reportOneVariant(year, outputDir, dynamicRateReport, "dynamic")

        recommendBetterAlternative(fixedRateReport, dynamicRateReport)


        println("######################################################")
        println("# Let's see the guide in ${File(outputDir, "<fixed/dynamic>_guide_$year.html").absolutePath}")
        println("######################################################")
    }

    private fun reportOneVariant(year: Int, outputDir: File, data: Report, dollarConversionSchema: String) {
        File(outputDir, "${dollarConversionSchema}_dividend_$year.pdf").writeBytes(data.getDividendPdf())
        File(outputDir, "${dollarConversionSchema}_rsu_$year.pdf").writeBytes(data.getRsuPdf())
        File(outputDir, "${dollarConversionSchema}_espp_$year.pdf").writeBytes(data.getEsppPdf())
        File(outputDir, "${dollarConversionSchema}_sales_$year.pdf").writeBytes(data.getSalesPdf())
        File(outputDir, "${dollarConversionSchema}_guide_$year.html").writeText(data.getGuide(), StandardCharsets.UTF_8)

        File(outputDir, "${dollarConversionSchema}_rsu_2024.pdf").writeBytes(data.getRsu2024Pdf())
        File(outputDir, "${dollarConversionSchema}_espp_2024.pdf").writeBytes(data.getEspp2024Pdf())
    }



    private fun parseExportFile(schwabExportFile: File): ParsedExport {
        return if (schwabExportFile.extension == "json") {
            JsonExportParser().parse(load(schwabExportFile))
        } else {
            throw IllegalArgumentException("only .json files are supported")
        }
    }

    private fun parseETradeDir(eTradeDir: File): ParsedExport {
        val rsuRecords = RsuPdfParser.parseDirectory(File(eTradeDir, "rsu"))
        val esppRecords = EsppPdfParser.parseDirectory(File(eTradeDir, "espp"))
        val dividentXlsFile = locateSingleFile(File(eTradeDir, "dividends"), "xlsx")
        val dividendXlsxResult = dividentXlsFile?.let {  DividendXlsxParser.parse(it)}
        val eTradeXlsFile = locateSingleFile(File(eTradeDir, "sales"), "xlsx")
        val eTradeCsvFile = locateSingleFile(File(eTradeDir, "sales"), "csv")

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

        println("######################################################")
        println("# Recommendation (If old legislative was used for 2024): ")
        println("# $recommendationOldLegislativeUsedIn2024")
        println("######################################################")
        println()



        val recomendationNewLegislativeUsed2024 = if (profit2024WhenUsedFixedRate+profitWhenUsedFixedRate <= profit2024WhenUsedDynamicRate+profitWhenUsedDynamicRate) {
            "Use fixed Dollar conversion rate  because " +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate)}+${FormatingHelper.formatDouble(profitWhenUsedFixedRate)}<=" +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate)}+${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)} " +
                    "(diff=${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate+profitWhenUsedDynamicRate - profit2024WhenUsedFixedRate-profit2024WhenUsedDynamicRate)})"
        } else {
            "Use dynamic Dollar conversion rate, because " +
                    "${FormatingHelper.formatDouble(profit2024WhenUsedDynamicRate)}+${FormatingHelper.formatDouble(profitWhenUsedDynamicRate)}" +
                    "<${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate)} +${FormatingHelper.formatDouble(profitWhenUsedFixedRate)} " +
                    "(diff=${FormatingHelper.formatDouble(profit2024WhenUsedFixedRate +profitWhenUsedFixedRate  - profit2024WhenUsedDynamicRate-profitWhenUsedDynamicRate)})"
        }

        println("######################################################")
        println("# Recommendation (If new legislative was used in 2024) ")
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