package cz.solutions.cockroach

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream

/** Definition of a single column in a PDF table. */
data class PdfColumn(val name: String, val width: Float)

/**
 * A group header cell that spans [colspan] columns.
 * Use empty [label] to leave the span blank (no background drawn for blank cells).
 * The sum of all colspans must equal the number of columns.
 */
data class ColumnGroupHeader(val label: String, val colspan: Int)

/** A single cell in the summary row. Use [bold] to highlight tax-relevant values. */
data class SummaryCell(val text: String, val bold: Boolean = false) {
    companion object {
        fun regular(text: String) = SummaryCell(text, false)
        fun bold(text: String) = SummaryCell(text, true)
        fun empty() = SummaryCell("", false)
    }
}

/**
 * Full definition of a tabular PDF report.
 *
 * @param title              bold heading rendered on the first page
 * @param subtitles          optional lines rendered below the title in smaller font
 * @param columnGroupHeaders optional group-header row above column names (colspan support)
 * @param columns            table column definitions
 * @param rows               data rows – each inner list must have the same size as [columns]
 * @param summaryRow         optional bold totals row (same size as [columns], use "" for empty cells)
 * @param footerLines        optional text lines rendered below the table
 * @param landscape          whether to use landscape A4 (default) or portrait
 */
data class PdfReportDefinition(
    val title: String,
    val subtitles: List<String> = emptyList(),
    val columnGroupHeaders: List<ColumnGroupHeader>? = null,
    val columns: List<PdfColumn>,
    val rows: List<List<String>>,
    val summaryRow: List<SummaryCell>? = null,
    val footerLines: List<String> = emptyList(),
    val landscape: Boolean = true
)

/** Generic PDF generator that renders any [PdfReportDefinition] into a byte array. */
object PdfReportGenerator {

    private const val MARGIN = 30f
    private const val ROW_HEIGHT = 14f
    private const val TITLE_FONT_SIZE = 14f
    private const val SUBTITLE_FONT_SIZE = 10f
    private const val TABLE_FONT_SIZE = 7f
    private const val FOOTER_FONT_SIZE = 8f

    fun generate(definition: PdfReportDefinition): ByteArray {
        val doc = PDDocument()
        try {
            val regularFont = loadFont(doc, "fonts/DejaVuSans.ttf")
            val boldFont = loadFont(doc, "fonts/DejaVuSans-Bold.ttf")
            val pageRect = if (definition.landscape) PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width) else PDRectangle.A4
            val pageHeight = pageRect.height
            val usableWidth = pageRect.width - 2 * MARGIN
            val totalWeight = definition.columns.sumOf { it.width.toDouble() }.toFloat()
            val columnWidths = definition.columns.map { it.width / totalWeight * usableWidth }
            val groupSeparators = computeGroupSeparators(definition.columnGroupHeaders, columnWidths)

            var cs: PDPageContentStream? = null
            var yPos = 0f
            var isFirstPage = true
            var tableTopY = 0f  // tracks top of the table area (including header) on the current page

            fun finalizePage() {
                val current = cs ?: return
                drawVerticalSeparators(current, groupSeparators, tableTopY, yPos + ROW_HEIGHT - 3f)
            }

            fun newPage(): PDPageContentStream {
                finalizePage()
                cs?.close()
                val newCs = PDPageContentStream(doc, PDPage(pageRect).also { doc.addPage(it) })
                cs = newCs
                yPos = pageHeight - MARGIN
                if (isFirstPage) {
                    drawText(newCs, boldFont, TITLE_FONT_SIZE, MARGIN, yPos, definition.title); yPos -= 20f
                    for (subtitle in definition.subtitles) { drawText(newCs, regularFont, SUBTITLE_FONT_SIZE, MARGIN, yPos, subtitle); yPos -= 16f }
                    if (definition.subtitles.isNotEmpty()) yPos -= 5f
                    isFirstPage = false
                }
                tableTopY = yPos - 3f + ROW_HEIGHT
                if (definition.columnGroupHeaders != null) {
                    drawGroupHeaderRow(newCs, boldFont, definition.columnGroupHeaders, columnWidths, yPos)
                    yPos -= ROW_HEIGHT
                }
                drawColumnHeaderRow(newCs, boldFont, definition.columns, columnWidths, yPos)
                yPos -= ROW_HEIGHT
                return newCs
            }

            fun ensurePage(): PDPageContentStream {
                val current = cs
                return if (current == null || yPos < MARGIN + ROW_HEIGHT) newPage() else current
            }

            var stream: PDPageContentStream
            for (row in definition.rows) { stream = ensurePage(); drawTableRow(stream, regularFont, columnWidths, row, yPos); yPos -= ROW_HEIGHT }
            if (definition.summaryRow != null) { stream = ensurePage(); drawSummaryRow(stream, regularFont, boldFont, columnWidths, definition.summaryRow, yPos); yPos -= ROW_HEIGHT }

            // Finalize last page separators
            finalizePage()

            // Footer lines (after separators, outside the table)
            if (definition.footerLines.isNotEmpty()) {
                yPos -= ROW_HEIGHT
                stream = ensurePage()
                for (line in definition.footerLines) { drawText(stream, regularFont, FOOTER_FONT_SIZE, MARGIN, yPos, line); yPos -= ROW_HEIGHT }
            }
            cs?.close()

            val baos = ByteArrayOutputStream()
            doc.save(baos)
            return baos.toByteArray()
        } finally {
            doc.close()
        }
    }

    private fun loadFont(doc: PDDocument, resourcePath: String): PDFont {
        val stream = PdfReportGenerator::class.java.getResourceAsStream(resourcePath) ?: throw IllegalStateException("Font resource not found: $resourcePath")
        return PDType0Font.load(doc, stream)
    }

    private fun drawText(cs: PDPageContentStream, font: PDFont, size: Float, x: Float, y: Float, text: String) {
        cs.beginText(); cs.setFont(font, size); cs.newLineAtOffset(x, y); cs.showText(text); cs.endText()
    }

    private fun drawGroupHeaderRow(cs: PDPageContentStream, font: PDFont, groups: List<ColumnGroupHeader>, columnWidths: List<Float>, yPos: Float) {
        val totalWidth = columnWidths.sum()
        var xPos = MARGIN
        var colIndex = 0
        for (group in groups) {
            val spanWidth = columnWidths.subList(colIndex, colIndex + group.colspan).sum()
            if (group.label.isNotEmpty()) {
                cs.setNonStrokingColor(0.78f, 0.78f, 0.78f)
                cs.addRect(xPos, yPos - 3f, spanWidth, ROW_HEIGHT)
                cs.fill()
                cs.setNonStrokingColor(0f, 0f, 0f)
                val textWidth = font.getStringWidth(group.label) / 1000f * TABLE_FONT_SIZE
                drawText(cs, font, TABLE_FONT_SIZE, xPos + (spanWidth - textWidth) / 2f, yPos, group.label)
            }
            xPos += spanWidth
            colIndex += group.colspan
        }
        cs.moveTo(MARGIN, yPos - 3f); cs.lineTo(MARGIN + totalWidth, yPos - 3f); cs.stroke()
    }

    private fun drawColumnHeaderRow(cs: PDPageContentStream, font: PDFont, columns: List<PdfColumn>, columnWidths: List<Float>, yPos: Float) {
        val totalWidth = columnWidths.sum()
        cs.setNonStrokingColor(0.85f, 0.85f, 0.85f); cs.addRect(MARGIN, yPos - 3f, totalWidth, ROW_HEIGHT); cs.fill(); cs.setNonStrokingColor(0f, 0f, 0f)
        var xPos = MARGIN
        for ((i, col) in columns.withIndex()) { drawText(cs, font, TABLE_FONT_SIZE, xPos + 2f, yPos, col.name); xPos += columnWidths[i] }
        cs.moveTo(MARGIN, yPos - 3f); cs.lineTo(MARGIN + totalWidth, yPos - 3f); cs.stroke()
    }

    private fun drawTableRow(cs: PDPageContentStream, font: PDFont, columnWidths: List<Float>, data: List<String>, yPos: Float) {
        var xPos = MARGIN
        for ((i, width) in columnWidths.withIndex()) { drawText(cs, font, TABLE_FONT_SIZE, xPos + 2f, yPos, if (i < data.size) data[i] else ""); xPos += width }
    }

    private fun drawSummaryRow(cs: PDPageContentStream, regularFont: PDFont, boldFont: PDFont, columnWidths: List<Float>, data: List<SummaryCell>, yPos: Float) {
        var xPos = MARGIN
        for ((i, width) in columnWidths.withIndex()) {
            val cell = if (i < data.size) data[i] else SummaryCell.empty()
            val font = if (cell.bold) boldFont else regularFont
            drawText(cs, font, TABLE_FONT_SIZE, xPos + 2f, yPos, cell.text)
            xPos += width
        }
    }

    /** Returns X positions of vertical separators at all group boundaries (including outer edges). */
    private fun computeGroupSeparators(groups: List<ColumnGroupHeader>?, columnWidths: List<Float>): List<Float> {
        if (groups == null || groups.size <= 1) return emptyList()
        val separators = mutableListOf(MARGIN)
        var colIndex = 0
        var xPos = MARGIN
        for (group in groups) {
            xPos += columnWidths.subList(colIndex, colIndex + group.colspan).sum()
            colIndex += group.colspan
            separators.add(xPos)
        }
        return separators
    }

    private fun drawVerticalSeparators(cs: PDPageContentStream, separators: List<Float>, yTop: Float, yBottom: Float) {
        if (separators.isEmpty()) return
        cs.setLineWidth(0.5f)
        for (x in separators) { cs.moveTo(x, yTop); cs.lineTo(x, yBottom) }
        cs.stroke()
        cs.setLineWidth(1f)
    }
}
