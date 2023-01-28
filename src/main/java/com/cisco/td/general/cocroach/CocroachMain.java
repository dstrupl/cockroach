package com.cisco.td.general.cocroach;

import com.cisco.td.ade.commandline.CommandLineApplication;
import com.cisco.td.ade.logging.ConsoleLogging;
import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.io.ByteSources;
import lombok.CustomLog;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

@ConsoleLogging
@CustomLog
public class CocroachMain extends CommandLineApplication {

    public void report(File schwabExportFile, int year, File outputDir) throws IOException {

        ExportParser exportParser = new ExportParser();
        ReportGenerator reportGenerator = new ReportGenerator();

        ParsedExport parsedExport = exportParser.parse(ByteSources.fromFile(schwabExportFile));
        Report fixedRateReport = reportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded());
        Report dynamicRateReport = reportGenerator.generateForYear(parsedExport, year, TabularExchangeRateProvider.hardcoded());

        Report data = chooseBetterAltertnative(fixedRateReport,dynamicRateReport);

        FileUtils.writeStringToFile(new File(outputDir, "dividend_" + year + ".md"), data.getDividend(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "rsu_" + year + ".md"), data.getRsu(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "espp_" + year + ".md"), data.getEspp(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "sales_" + year + ".md"), data.getSales(), StandardCharsets.UTF_8);


        LOGGER.infoGlobal("35 Úhrn příjmů plynoucí ze zahraničí podle § 6 zákona (o tuto castku je traba navysit radek 31) : {}",FormatingHelper.formatDouble(data.taxableIncome()));
        LOGGER.infoGlobal("38 Dílčí základ daně z kapitálového majetku podle § 8 zákona : {}",FormatingHelper.formatDouble(data.taxableDividendIncome()));
        LOGGER.infoGlobal("323 Daň zaplacená v zahraničí : {}",FormatingHelper.formatDouble(data.payedDividendTax()));


    }

    private Report chooseBetterAltertnative(Report fixedRateReport ,Report dynamicRateReport){
        double taxWhenUsedFixedRate = fixedRateReport.taxToPay();
        double taxWhenUsedDynamicRate = dynamicRateReport.taxToPay();
        if(taxWhenUsedFixedRate<=taxWhenUsedDynamicRate){
            LOGGER.infoGlobal(
                    "Using fixed Dollar conversion rate, because {}<={} (diff={})",
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate-taxWhenUsedFixedRate)
            );
            return fixedRateReport;
        } else{
            LOGGER.infoGlobal(
                    "Using dynamic Dollar conversion rate, because {}<{} (diff={})",
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate-taxWhenUsedDynamicRate)
            );
            return dynamicRateReport;
        }
    }
}
