package com.cisco.td.general.cocroach;

import com.cisco.td.ade.commandline.CommandLineApplication;
import com.cognitivesecurity.commons.io.ByteSources;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CocroachMain extends CommandLineApplication {

    public void report(File schwabExportFile, int year, File outputDir) throws IOException {

        ExportParser exportParser = new ExportParser();
        ReportGenerator reportGenerator = new ReportGenerator();

        ParsedExport parsedExport = exportParser.parse(ByteSources.fromFile(schwabExportFile));
        Report data = reportGenerator.generateForYear(parsedExport, year);

        FileUtils.writeStringToFile(new File(outputDir, "dividend_" + year + ".md"), data.getDividend(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, "rsu_" + year + ".md"), data.getRsu(), StandardCharsets.UTF_8);

    }
}
