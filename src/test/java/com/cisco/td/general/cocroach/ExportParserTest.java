package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.io.ByteSources;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ExportParserTest {

    @Test
    void canParse() throws IOException {
        ExportParser exportParser = new ExportParser();

        ParsedExport parsedExport = exportParser.parse(ByteSources.fromFile(new File("/Users/jandryse/Downloads/EquityAwardsCenter_Transactions_2021811145652.csv")));


        ReportGenerator reportGenerator = new ReportGenerator();

        String data = reportGenerator.generateForYear(parsedExport, 2020);

        FileUtils.writeStringToFile(new File("/tmp/report.md"),data, StandardCharsets.UTF_8);
        //System.out.println(parsedExport);
    }
}