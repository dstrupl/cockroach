package com.cisco.td.general.cocroach;

import com.cisco.td.ade.commandline.CommandLineApplication;
import com.cisco.td.ade.logging.ConsoleLogging;
import com.cognitivesecurity.commons.collections.MoreFluentIterable;
import com.cognitivesecurity.commons.io.ByteSources;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

@ConsoleLogging
public class CocroachMain extends CommandLineApplication {

    public void report(File schwabExportFile, int year, File outputDir) throws IOException {

        ExportParser exportParser = new ExportParser();
        ReportGenerator reportGenerator = new ReportGenerator();

        ParsedExport parsedExport = exportParser.parse(ByteSources.fromFile(schwabExportFile));
        Report fixedRateReport = reportGenerator.generateForYear(parsedExport, year, YearConstantExchangeRateProvider.hardcoded());
        Report dynamicRateReport = reportGenerator.generateForYear(parsedExport, year, TabularExchangeRateProvider.hardcoded());

        Report data = MoreFluentIterable.of(fixedRateReport, dynamicRateReport).checkMin(Comparator.comparingDouble(Report::taxToPay));

        FileUtils.writeStringToFile(new File(outputDir, "dividend_" + year + ".md"), data.getDividend(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "rsu_" + year + ".md"), data.getRsu(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "espp_" + year + ".md"), data.getEspp(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "sales_" + year + ".md"), data.getSales(), StandardCharsets.UTF_8);

    }
}
