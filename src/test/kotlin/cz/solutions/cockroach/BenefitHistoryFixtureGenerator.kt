package cz.solutions.cockroach

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream

/**
 * Builds a synthetic E-Trade BenefitHistory.xlsx fixture that mirrors the column layout of a real
 * export from the Morgan Stanley Stock Plan Connect portal (sheet names, header rows, the
 * Grant/Vest Schedule/Tax Withholding row sequence, and the duplicated "Vested Qty." header that
 * [ETradeBenefitHistoryParser] relies on). Symbols, grant ids, quantities, and amounts are all
 * anonymised; only the structural layout is preserved.
 */
object BenefitHistoryFixtureGenerator {

    private val ESPP_HEADER = listOf(
        "Record Type", "Symbol", "Purchase Date", "Purchase Price", "Purchased Qty.",
        "Tax Collection Shares", "Net Shares", "Sellable Qty.", "Est. Market Value",
        "Grant Date", "Discount Percent", "Grant Date FMV", "Purchase Date FMV",
        "Qualified Plan?", "Contribution Source", "Pending Sale Qty.", "Blocked Qty.",
        "Transferable Date", "First Sellable Date", "Date", "Event Type", "Qty"
    )

    private val RSU_HEADER = listOf(
        "Record Type", "Symbol", "Grant Date", "Settlement Type", "Granted Qty.",
        "Withheld Qty.", "Vested Qty.", "Deferred / Pending Release Qty.", "Sellable Qty.",
        "Est. Market Value", "Grant Number", "Achieved Qty.", "Unvested Qty.", "Type",
        "Unreleased Dividend Value", "Award Price", "Class", "Status", "Pending Sale Qty.",
        "Blocked Qty.", "Cancelled Qty.", "Date", "Event Type", "Qty. or Amount",
        "Vest Period", "Vest Date", "Deferred Until", "Granted Qty.", "Achieved Qty.",
        "Reason for cancelled qty", "Cancelled Qty.", "Date Cancelled", "Vested Qty.",
        "Released Qty", "Released Amount", "Sellable Qty.", "Blocked Qty.",
        "Total Taxes Paid", "Tax Description", "Taxable Gain", "Effective Tax Rate",
        "Withholding Amount"
    )

    private const val SYMBOL = "ACME"

    fun write(out: OutputStream) {
        XSSFWorkbook().use { wb ->
            writeEsppSheet(wb.createSheet("ESPP"))
            writeRsuSheet(wb.createSheet("Restricted Stock"))
            wb.write(out)
        }
    }

    private fun writeEsppSheet(sheet: Sheet) {
        writeHeader(sheet, ESPP_HEADER)
        // Three purchases: one in 2025-Q1, one in 2025-Q3, one in 2026-Q1. Each followed by an Event row.
        var r = 1
        r = esppPurchase(sheet, r, date = "15-MAR-2025", purchaseFmv = 15.00)
        r = esppEvent(sheet, r, date = "03/15/2025")
        r = esppPurchase(sheet, r, date = "15-SEP-2025", purchaseFmv = 20.00)
        r = esppEvent(sheet, r, date = "09/15/2025")
        r = esppPurchase(sheet, r, date = "15-MAR-2026", purchaseFmv = 18.00)
        r = esppEvent(sheet, r, date = "03/15/2026")
        // Totals row (parser ignores it; included for fidelity).
        sheet.createRow(r).also { setText(it, 0, "Totals"); setNum(it, 4, 300.0) }
    }

    private fun esppPurchase(sheet: Sheet, rowIdx: Int, date: String, purchaseFmv: Double): Int {
        val row = sheet.createRow(rowIdx)
        setText(row, 0, "Purchase"); setText(row, 1, SYMBOL); setText(row, 2, date)
        setNum(row, 3, 10.00); setNum(row, 4, 100.0); setNum(row, 11, 12.00); setNum(row, 12, purchaseFmv)
        return rowIdx + 1
    }

    private fun esppEvent(sheet: Sheet, rowIdx: Int, date: String): Int {
        val row = sheet.createRow(rowIdx)
        setText(row, 0, "Event"); setText(row, 19, date); setText(row, 20, "PURCHASE"); setNum(row, 21, 100.0)
        return rowIdx + 1
    }

    private fun writeRsuSheet(sheet: Sheet) {
        writeHeader(sheet, RSU_HEADER)
        var r = 1
        // Grant G-1001 - small grant, four vested tranches across 2025-2026.
        r = rsuGrant(sheet, r, "G-1001")
        r = rsuVestPair(sheet, r, "G-1001", period = 1, vestDate = "06/20/2025", qty = 50, taxableGain = 750.00)
        r = rsuVestPair(sheet, r, "G-1001", period = 2, vestDate = "09/20/2025", qty = 50, taxableGain = 1000.00)
        r = rsuVestPair(sheet, r, "G-1001", period = 3, vestDate = "12/20/2025", qty = 50, taxableGain = 1100.00)
        r = rsuVestPair(sheet, r, "G-1001", period = 4, vestDate = "03/20/2026", qty = 50, taxableGain = 1250.00)
        // Grant G-1002 - larger grant, four vested tranches.
        r = rsuGrant(sheet, r, "G-1002")
        r = rsuVestPair(sheet, r, "G-1002", period = 1, vestDate = "06/20/2025", qty = 200, taxableGain = 3000.00)
        r = rsuVestPair(sheet, r, "G-1002", period = 2, vestDate = "09/20/2025", qty = 200, taxableGain = 4000.00)
        r = rsuVestPair(sheet, r, "G-1002", period = 3, vestDate = "12/20/2025", qty = 200, taxableGain = 4400.00)
        r = rsuVestPair(sheet, r, "G-1002", period = 4, vestDate = "03/20/2026", qty = 200, taxableGain = 5000.00)
        // Grant G-1003 - future grant with no vested shares (parser must skip it entirely).
        r = rsuGrant(sheet, r, "G-1003")
        r = rsuVestScheduleRow(sheet, r, "G-1003", period = 1, vestDate = "06/20/2027", qty = 0)
        r = rsuVestScheduleRow(sheet, r, "G-1003", period = 2, vestDate = "09/20/2027", qty = 0)
        sheet.createRow(r).also { setText(it, 0, "Totals") }
    }

    private fun rsuGrant(sheet: Sheet, rowIdx: Int, grantId: String): Int {
        val row = sheet.createRow(rowIdx)
        setText(row, 0, "Grant"); setText(row, 1, SYMBOL); setText(row, 10, grantId)
        return rowIdx + 1
    }

    private fun rsuVestPair(sheet: Sheet, rowIdx: Int, grantId: String, period: Int, vestDate: String, qty: Int, taxableGain: Double): Int {
        val schedule = sheet.createRow(rowIdx)
        setText(schedule, 0, "Vest Schedule"); setText(schedule, 10, grantId)
        setNum(schedule, 24, period.toDouble()); setText(schedule, 25, vestDate); setNum(schedule, 32, qty.toDouble())
        val tax = sheet.createRow(rowIdx + 1)
        setText(tax, 0, "Tax Withholding"); setText(tax, 10, grantId)
        setNum(tax, 24, period.toDouble()); setText(tax, 38, "Czech"); setNum(tax, 39, taxableGain)
        return rowIdx + 2
    }

    private fun rsuVestScheduleRow(sheet: Sheet, rowIdx: Int, grantId: String, period: Int, vestDate: String, qty: Int): Int {
        val row = sheet.createRow(rowIdx)
        setText(row, 0, "Vest Schedule"); setText(row, 10, grantId)
        setNum(row, 24, period.toDouble()); setText(row, 25, vestDate); setNum(row, 32, qty.toDouble())
        return rowIdx + 1
    }

    private fun writeHeader(sheet: Sheet, headers: List<String>) {
        val row = sheet.createRow(0)
        headers.forEachIndexed { i, name -> setText(row, i, name) }
    }

    private fun setText(row: Row, col: Int, value: String) {
        row.createCell(col).setCellValue(value)
    }

    private fun setNum(row: Row, col: Int, value: Double) {
        row.createCell(col).setCellValue(value)
    }
}
