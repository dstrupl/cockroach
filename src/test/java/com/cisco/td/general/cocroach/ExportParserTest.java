package com.cisco.td.general.cocroach;

import com.cognitivesecurity.commons.io.ByteSources;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ExportParserTest {

    @Test
    void canParse() {
        ExportParser exportParser = new ExportParser();

        ParsedExport parsedExport = exportParser.parse(ByteSources.fromFile(new File("/Users/jandryse/Downloads/EquityAwardsCenter_Transactions_2021811145652.csv")));

        System.out.println(parsedExport);
    }
}