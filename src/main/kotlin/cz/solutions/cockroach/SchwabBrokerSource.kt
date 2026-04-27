package cz.solutions.cockroach

import java.io.File

class SchwabBrokerSource(private val jsonFile: File) : BrokerSource {
    override val name: String = "Schwab"

    override fun parse(): ParsedExport {
        require(jsonFile.extension == "json") { "Schwab export must be a .json file: ${jsonFile.absolutePath}" }
        return JsonExportParser().parse(loadText(jsonFile))
    }
}
