package cz.solutions.cockroach;

import lombok.Value;

import java.util.List;

@Value
public class ParsedExport {
    List<RsuRecord> rsuRecords;
    List<EsppRecord> esppRecords;
    List<DividendRecord> dividendRecords;
    List<TaxRecord> taxRecords;
    List<TaxReversalRecord> taxReversalRecords;
    List<SaleRecord> saleRecords;
    List<JournalRecord> journalRecords;
}
