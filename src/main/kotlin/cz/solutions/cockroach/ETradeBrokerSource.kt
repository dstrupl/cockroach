package cz.solutions.cockroach

import java.io.File

/**
 * E-Trade is unusual: it accepts a directory laid out with `rsu/`, `espp/`, `dividends/` and
 * `sales/` subdirectories, an optional benefit-history XLSX that supersedes the RSU/ESPP PDFs,
 * or any combination of the two. At least one of [directory] or [benefitHistoryFile] must be set.
 */
class ETradeBrokerSource(
    private val directory: File?,
    private val benefitHistoryFile: File? = null,
) : BrokerSource {
    override val name: String = "E-Trade"

    // E-Trade was acquired by Morgan Stanley; the legal counterparty on Příloha č. 3 / § 6 reports
    // is Morgan Stanley, matching what ETradeBenefitHistoryParser/ETradeGainLossParser emit. The
    // RSU/ESPP PDFs themselves are still in the legacy Schwab template and carry no broker name,
    // so we stamp them here.
    private val brokerName: String = "Morgan Stanley & Co."

    init {
        require(directory != null || benefitHistoryFile != null) {
            "E-Trade source needs either a directory or a benefit-history file"
        }
    }

    override fun parse(): ParsedExport {
        val benefitHistory = benefitHistoryFile?.let { ETradeBenefitHistoryParser.parse(it) }
        val rsuRecords = benefitHistory?.rsuRecords
            ?: directory?.let { RsuPdfParser.parseDirectory(File(it, "rsu"), brokerName) }
            ?: emptyList()
        val esppRecords = benefitHistory?.esppRecords
            ?: directory?.let { EsppPdfParser.parseDirectory(File(it, "espp"), brokerName) }
            ?: emptyList()
        val dividendXlsxFile = directory?.let { locateSingleFile(File(it, "dividends"), "xlsx") }
        val dividendXlsxResult = dividendXlsxFile?.let { DividendXlsxParser.parse(it) }
        val salesXlsxFile = directory?.let { locateSingleFile(File(it, "sales"), "xlsx") }
        val salesCsvFile = directory?.let { locateSingleFile(File(it, "sales"), "csv") }

        return ParsedExport(
            rsuRecords = rsuRecords,
            esppRecords = esppRecords,
            saleRecords = salesXlsxFile?.let { ETradeGainLossXlsParser.parse(it) }
                ?: salesCsvFile?.let { ETradeGainLossParser.parse(loadText(it)) }
                ?: emptyList(),
            dividendRecords = dividendXlsxResult?.dividendRecords ?: emptyList(),
            taxRecords = dividendXlsxResult?.taxRecords ?: emptyList(),
            taxReversalRecords = emptyList(),
            journalRecords = emptyList()
        )
    }

    private fun locateSingleFile(directory: File, extension: String): File? {
        if (!directory.exists()) {
            return null
        }
        require(directory.isDirectory) { "${directory.absolutePath} is not a directory" }
        val files = directory.listFiles { file ->
            !file.isHidden && !file.name.startsWith("~") && !file.name.startsWith(".") &&
                    file.extension.equals(extension, ignoreCase = true)
        }?.toList() ?: emptyList()
        require(files.size <= 1) {
            "Expected max one .$extension file in ${directory.absolutePath}, " +
                    "but found ${files.size}: ${files.map { it.name }}"
        }
        return files.firstOrNull()
    }
}
