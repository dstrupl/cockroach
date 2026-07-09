package cz.solutions.cockroach

import java.io.File

class VubBrokerSource(
    private val files: List<File>,
    private val year: Int,
) : BrokerSource {
    override val name: String = "VÚB"

    override fun parse(): ParsedExport =
        files.map { parseSingleFile(it) }.fold(ParsedExport.empty()) { acc, e -> acc + e }

    private fun parseSingleFile(file: File): ParsedExport {
        val interestRecords = VubInterestPdfParser.parse(file, year)
        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = emptyList(),
            taxRecords = emptyList(),
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList(),
            interestRecords = interestRecords
        )
    }
}
