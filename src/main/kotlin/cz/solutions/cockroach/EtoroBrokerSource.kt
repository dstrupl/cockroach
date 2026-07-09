package cz.solutions.cockroach

import java.io.File

class EtoroBrokerSource(private val files: List<File>) : BrokerSource {
    override val name: String = "eToro"

    override fun parse(): ParsedExport =
        files.map { parseSingleFile(it) }.fold(ParsedExport.empty()) { acc, e -> acc + e }

    private fun parseSingleFile(file: File): ParsedExport {
        val result = EtoroXlsxParser.parse(file)
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
}
