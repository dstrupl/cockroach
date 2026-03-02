package cz.solutions.cockroach

data class ParsedExport(
    val rsuRecords: List<RsuRecord>,
    val esppRecords: List<EsppRecord>,
    val dividendRecords: List<DividendRecord>,
    val taxRecords: List<TaxRecord>,
    val taxReversalRecords: List<TaxReversalRecord>,
    val saleRecords: List<SaleRecord>,
    val journalRecords: List<JournalRecord>
) {
    operator fun plus(other: ParsedExport): ParsedExport {
        return ParsedExport(
            rsuRecords = rsuRecords + other.rsuRecords,
            esppRecords = esppRecords + other.esppRecords,
            dividendRecords = dividendRecords + other.dividendRecords,
            taxRecords = taxRecords + other.taxRecords,
            taxReversalRecords = taxReversalRecords + other.taxReversalRecords,
            saleRecords = saleRecords + other.saleRecords,
            journalRecords = journalRecords + other.journalRecords
        )
    }

    companion object {
        fun empty(): ParsedExport = ParsedExport(
            emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList()
        )
    }
}