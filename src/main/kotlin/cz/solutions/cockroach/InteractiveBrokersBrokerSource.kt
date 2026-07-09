package cz.solutions.cockroach

import java.io.File

/**
 * Interactive Brokers source for Transaction History dividend CSV exports.
 *
 * The configured directory follows the legacy PR #11 layout and contains a
 * `dividends/` subdirectory with at most one CSV export.
 */
class InteractiveBrokersBrokerSource(private val directory: File) : BrokerSource {
    override val name: String = "Interactive Brokers"

    override fun parse(): ParsedExport {
        val dividendCsvFile = locateSingleCsv(File(directory, "dividends"))
        val result = dividendCsvFile?.let { DividendIBParser.parse(it) }

        return ParsedExport(
            rsuRecords = emptyList(),
            esppRecords = emptyList(),
            dividendRecords = result?.dividendRecords.orEmpty(),
            taxRecords = result?.taxRecords.orEmpty(),
            taxReversalRecords = emptyList(),
            saleRecords = emptyList(),
            journalRecords = emptyList(),
        )
    }

    private fun locateSingleCsv(dividendsDir: File): File? {
        if (!dividendsDir.exists()) return null
        require(dividendsDir.isDirectory) { "${dividendsDir.absolutePath} is not a directory" }
        val files = dividendsDir.listFiles { file ->
            !file.isHidden && !file.name.startsWith("~") && !file.name.startsWith(".") &&
                    file.extension.equals("csv", ignoreCase = true)
        }?.toList().orEmpty()
        require(files.size <= 1) {
            "Expected max one .csv file in ${dividendsDir.absolutePath}, " +
                    "but found ${files.size}: ${files.map { it.name }}"
        }
        return files.firstOrNull()
    }
}
