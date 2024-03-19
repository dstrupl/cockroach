package com.cisco.td.general.cocroach;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


public class CocroachMain {
    private static final Logger LOGGER = Logger.getLogger(CocroachMain.class.getName());
    public void report(File schwabExportFile, int year, File outputDir) throws IOException {

        ReportGenerator reportGenerator = new ReportGenerator();

        ParsedExport parsedExport = parseExportFile(schwabExportFile);
        Report fixedRateReport = reportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded());
        Report dynamicRateReport = reportGenerator.generateForYear(parsedExport, year, TabularExchangeRateProvider.hardcoded());

        Report data = chooseBetterAltertnative(fixedRateReport,dynamicRateReport);

        FileUtils.writeStringToFile(new File(outputDir, "dividend_" + year + ".md"), data.getDividend(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "rsu_" + year + ".md"), data.getRsu(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "espp_" + year + ".md"), data.getEspp(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "sales_" + year + ".md"), data.getSales(), StandardCharsets.UTF_8);


        LOGGER.info("35 Úhrn příjmů plynoucí ze zahraničí podle § 6 zákona (o tuto castku je traba navysit radek 31) : {}",FormatingHelper.formatDouble(data.taxableIncome()));
        LOGGER.info("38 Dílčí základ daně z kapitálového majetku podle § 8 zákona : {}",FormatingHelper.formatDouble(data.taxableDividendIncome()));
        LOGGER.info("323 Daň zaplacená v zahraničí : {}",FormatingHelper.formatDouble(data.payedDividendTax()));
    }

    private ParsedExport parseExportFile(File schwabExportFile){
        String extension = FilenameUtils.getExtension(schwabExportFile.getName());

        if(extension.equals("json")) {
            JsonExportParser exportParser = new JsonExportParser();
            return exportParser.parse(ByteSources.fromFile(schwabExportFile));
        } else if(extension.equals("csv")){
            LOGGER.warn("You are using legacy version based on CSV export. This functionality will be removed. Nex time, please pass JSON export file!");
            ExportParser exportParser = new ExportParser();
            return exportParser.parse(ByteSources.fromFile(schwabExportFile));
        }else{
            throw new IllegalArgumentException("only .json .csv files are supported");
        }

    }

    private Report chooseBetterAltertnative(Report fixedRateReport ,Report dynamicRateReport){
        double taxWhenUsedFixedRate = fixedRateReport.taxToPay();
        double taxWhenUsedDynamicRate = dynamicRateReport.taxToPay();
        if(taxWhenUsedFixedRate<=taxWhenUsedDynamicRate){
            LOGGER.info(
                    "Using fixed Dollar conversion rate, because {}<={} (diff={})",
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate-taxWhenUsedFixedRate)
            );
            return fixedRateReport;
        } else{
            LOGGER.info(
                    "Using dynamic Dollar conversion rate, because {}<{} (diff={})",
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate-taxWhenUsedDynamicRate)
            );
            return dynamicRateReport;
        }
    }

    public static void main(String[] args) throws IOException {
        CocroachMain cocroachMain = new CocroachMain();
        cocroachMain.report(new File(args[0]), Integer.parseInt(args[1]), new File(args[2]));
    }
}
