package cz.solutions.cockroach

data class ParsedExport(
    val rsuRecords: List<RsuRecord>,
    val esppRecords: List<EsppRecord>,
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>,
    val taxReversalRecords: List<TaxReversalRecord>,
    val saleRecords: List<SaleRecord>,
    val journalRecords: List<JournalRecord>
)