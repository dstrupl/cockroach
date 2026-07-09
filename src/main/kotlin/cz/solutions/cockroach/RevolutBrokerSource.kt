package cz.solutions.cockroach

import java.io.File

class RevolutBrokerSource(
    private val stocksFiles: List<File>,
    private val savingsFiles: List<File>,
    private val whtRate: Double = RevolutParser.DEFAULT_WHT_RATE,
) : BrokerSource {
    override val name: String = "Revolut"

    override fun parse(): ParsedExport {
        val stocks = stocksFiles.map { parseStocksFile(it) }
            .fold(ParsedExport.empty()) { acc, e -> acc + e }
        val savings = savingsFiles.map { parseSavingsFile(it) }
            .fold(ParsedExport.empty()) { acc, e -> acc + e }
        return stocks + savings
    }

    private fun parseStocksFile(file: File): ParsedExport {
        val result = RevolutParser.parseStocks(file, whtRate)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = result.dividendRecords,
            taxRecords = result.taxRecords,
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun parseSavingsFile(file: File): ParsedExport {
        val result = RevolutParser.parseSavings(file)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = emptyList(),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList(),
            interestRecords = result.interestRecords
        )
    }
}
