package cz.solutions.cockroach;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Logger;


public class CockroachMain {
    private static final Logger LOGGER = Logger.getLogger(CockroachMain.class.getName());
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


        LOGGER.info(MessageFormat.format(
                "35 Úhrn příjmů plynoucí ze zahraničí podle § 6 zákona (o tuto castku je traba navysit radek 31) : {0}",
                FormatingHelper.formatDouble(data.taxableIncome())));
        LOGGER.info(MessageFormat.format(
                "38 Dílčí základ daně z kapitálového majetku podle § 8 zákona : {0}",
                FormatingHelper.formatDouble(data.taxableDividendIncome())));
        LOGGER.info(MessageFormat.format(
                "323 Daň zaplacená v zahraničí : {0}",
                FormatingHelper.formatDouble(data.payedDividendTax())));
    }

    private ParsedExport parseExportFile(File schwabExportFile) throws IOException {
        String extension = FilenameUtils.getExtension(schwabExportFile.getName());

        if(extension.equals("json")) {
            JsonExportParser exportParser = new JsonExportParser();
            return exportParser.parse(load(schwabExportFile));
        } else if(extension.equals("csv")){
            LOGGER.warning("You are using legacy version based on CSV export. This functionality will be removed. Nex time, please pass JSON export file!");
            ExportParser exportParser = new ExportParser();
            return exportParser.parse(load(schwabExportFile));
        }else{
            throw new IllegalArgumentException("only .json .csv files are supported");
        }

    }

    private Report chooseBetterAltertnative(Report fixedRateReport ,Report dynamicRateReport){
        double taxWhenUsedFixedRate = fixedRateReport.taxToPay();
        double taxWhenUsedDynamicRate = dynamicRateReport.taxToPay();
        if(taxWhenUsedFixedRate<=taxWhenUsedDynamicRate){
            LOGGER.info(MessageFormat.format(
                    "Using fixed Dollar conversion rate, because {0}<={1} (diff={2})",
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate-taxWhenUsedFixedRate)
            ));
            return fixedRateReport;
        } else{
            LOGGER.info(MessageFormat.format(
                    "Using dynamic Dollar conversion rate, because {0}<{1} (diff={2})",
                    FormatingHelper.formatDouble(taxWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate),
                    FormatingHelper.formatDouble(taxWhenUsedFixedRate-taxWhenUsedDynamicRate)
            ));
            return dynamicRateReport;
        }
    }
    public static String load(File file) {
        try {
            InputStream is = FileUtils.openInputStream(file);
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Could not load file " + file.getAbsolutePath(), e);
        }
    }
    public static void main(String[] args) throws IOException {
        CockroachMain cockroachMain = new CockroachMain();
        cockroachMain.report(new File(args[0]), Integer.parseInt(args[1]), new File(args[2]));
    }
}
