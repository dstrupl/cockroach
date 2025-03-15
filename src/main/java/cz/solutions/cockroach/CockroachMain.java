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


        reportOneVariant(year, outputDir, fixedRateReport,"fixed");
        reportOneVariant(year, outputDir, dynamicRateReport,"dynamic");

        recommendBetterAlternative(fixedRateReport,dynamicRateReport,true);
        recommendBetterAlternative(fixedRateReport,dynamicRateReport,false);



        System.out.println("######################################################");
        System.out.println("# Let's see the guide in" + new File(outputDir, "<fixed/dynamic>_guide_" + year + ".html").getAbsolutePath());
        System.out.println("######################################################");

    }

    private void reportOneVariant(int year, File outputDir,Report data,String dollarConversionSchema) throws IOException {
        FileUtils.writeStringToFile(new File(outputDir, dollarConversionSchema+"_dividend_" + year + ".md"), data.getDividend(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, dollarConversionSchema+"_rsu_" + year + ".md"), data.getRsu(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, dollarConversionSchema+"_espp_" + year + ".md"), data.getEspp(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, dollarConversionSchema+"_sales_" + year + ".md"), data.getSales(), StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(outputDir, dollarConversionSchema+"_guide_" + year + ".html"), data.getGuide(), StandardCharsets.UTF_8);
    }

    private ParsedExport parseExportFile(File schwabExportFile) throws IOException {
        String extension = FilenameUtils.getExtension(schwabExportFile.getName());

        if(extension.equals("json")) {
            JsonExportParser exportParser = new JsonExportParser();
            return exportParser.parse(load(schwabExportFile));
        }else{
            throw new IllegalArgumentException("only .json  files are supported");
        }

    }

    private void recommendBetterAlternative(Report fixedRateReport ,Report dynamicRateReport, boolean use2024Legislative) {
        double profitWhenUsedFixedRate = fixedRateReport.rsuAndEsppAndSalesProfitCroneValue(use2024Legislative);
        double profitWhenUsedDynamicRate = dynamicRateReport.rsuAndEsppAndSalesProfitCroneValue(use2024Legislative);
        String recommendation;
        if(profitWhenUsedFixedRate<=profitWhenUsedDynamicRate){
            recommendation= MessageFormat.format(
                    "Use fixed Dollar conversion rate, because {0}<={1} (diff={2})",
                    FormatingHelper.formatDouble(profitWhenUsedFixedRate),
                    FormatingHelper.formatDouble(profitWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(profitWhenUsedDynamicRate-profitWhenUsedFixedRate)
            );

        } else{
            recommendation= MessageFormat.format(
                    "Use dynamic Dollar conversion rate, because {0}<{1} (diff={2})",
                    FormatingHelper.formatDouble(profitWhenUsedDynamicRate),
                    FormatingHelper.formatDouble(profitWhenUsedFixedRate),
                    FormatingHelper.formatDouble(profitWhenUsedFixedRate-profitWhenUsedDynamicRate)
            );
        }

        System.out.println("######################################################");
        if(use2024Legislative) {
            System.out.println("# Recommendation with 2024 legislative: ");
        }else{
            System.out.println("# Recommendation with old legislative: ");
        }
        System.out.println("# " + recommendation);
        System.out.println("######################################################");
        System.out.printf("\n");
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
