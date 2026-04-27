package cz.solutions.cockroach

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.data.Offset.offset
import org.joda.time.LocalDate
import org.junit.jupiter.api.Test
import java.io.StringReader

class RevolutParserTest {

    @Test
    fun parsesStocksDividendsWithGrossUpAtDefaultWhtRate() {
        val csv = """
            Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency,FX Rate
            2025-01-09T09:16:47.755556Z,MRK,DIVIDEND,,,USD 8.50,USD,0.0412
            2025-02-04T13:30:09.298662Z,T,DIVIDEND,,,USD 3.47,USD,0.0412
        """.trimIndent()

        val result = RevolutParser.parseStocks(StringReader(csv))

        assertThat(result.dividendRecords).hasSize(2)
        assertThat(result.dividendRecords[0].date).isEqualTo(LocalDate(2025, 1, 9))
        assertThat(result.dividendRecords[0].amount).isCloseTo(10.0, offset(0.0001))
        assertThat(result.dividendRecords[0].currency).isEqualTo(Currency.USD)
        assertThat(result.taxRecords).hasSize(2)
        assertThat(result.taxRecords[0].date).isEqualTo(LocalDate(2025, 1, 9))
        assertThat(result.taxRecords[0].amount).isCloseTo(-1.50, offset(0.0001))
        assertThat(result.taxRecords[0].currency).isEqualTo(Currency.USD)
    }

    @Test
    fun stocksWhtRateZeroProducesNoTaxRecords() {
        val csv = """
            Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency,FX Rate
            2025-01-09T09:16:47.755556Z,MRK,DIVIDEND,,,USD 8.50,USD,0.0412
        """.trimIndent()

        val result = RevolutParser.parseStocks(StringReader(csv), whtRate = 0.0)

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.dividendRecords[0].amount).isCloseTo(8.50, offset(0.0001))
        assertThat(result.taxRecords).isEmpty()
    }

    @Test
    fun stocksIgnoresCashWithdrawalAndCancellingTaxCorrectionPairs() {
        val csv = """
            Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency,FX Rate
            2025-06-27T07:59:16.209477Z,JNJ,DIVIDEND TAX (CORRECTION),,,USD -0.29,USD,0.0475
            2025-06-27T07:59:16.300231Z,JNJ,DIVIDEND TAX (CORRECTION),,,USD 0.29,USD,0.0475
            2025-10-02T05:40:01.919310Z,,CASH WITHDRAWAL,,,USD -96.39,USD,0.0485
            2025-12-30T12:39:09.676817Z,GILD,DIVIDEND,,,USD 4.38,USD,0.0487
        """.trimIndent()

        val result = RevolutParser.parseStocks(StringReader(csv))

        assertThat(result.dividendRecords).hasSize(1)
        assertThat(result.dividendRecords[0].date).isEqualTo(LocalDate(2025, 12, 30))
        assertThat(result.taxRecords).hasSize(1)
    }

    @Test
    fun stocksFailsLoudlyOnNonUsTickerSuffix() {
        val csv = """
            Date,Ticker,Type,Quantity,Price per share,Total Amount,Currency,FX Rate
            2025-03-15T09:00:00.000000Z,VOD.L,DIVIDEND,,,USD 1.70,USD,0.0420
        """.trimIndent()

        assertThatIllegalStateException()
            .isThrownBy { RevolutParser.parseStocks(StringReader(csv)) }
            .withMessageContaining("VOD.L")
            .withMessageContaining("looks non-US")
            .withMessageContaining("per-broker")
    }

    @Test
    fun parsesSavingsInterestAndIgnoresFeesAndBuySell() {
        val csv = """
            Date,Description,"Value, USD","Value, CZK",FX Rate,Price per share,Quantity of shares
            "Dec 31, 2025, 1:51:12 AM",Service Fee Charged USD Class IE000H9J0QX4,-1.2993,-26.7562,20.5923,,
            "Dec 31, 2025, 1:51:12 AM",Interest PAID USD Class R IE000H9J0QX4,3.4493,71.0291,20.5923,,
            "Dec 30, 2025, 1:50:13 AM",Interest PAID USD Class R IE000H9J0QX4,3.4393,71.0042,20.6450,,
            "Dec 29, 2025, 1:49:55 AM",BUY USD Class R IE000H9J0QX4,500.0000,10300.0000,20.6000,1.000,500
            "Dec 28, 2025, 1:49:41 AM",SELL USD Class R IE000H9J0QX4,-1130.0000,-23278.0000,20.6000,1.000,-1130
            "Dec 27, 2025, 1:48:53 AM",Interest Reinvested Class R USD IE000H9J0QX4,-3.4193,-70.5000,20.6000,,
        """.trimIndent()

        val result = RevolutParser.parseSavings(StringReader(csv))

        assertThat(result.interestRecords).hasSize(2)
        assertThat(result.interestRecords[0].date).isEqualTo(LocalDate(2025, 12, 31))
        assertThat(result.interestRecords[0].amount).isCloseTo(3.4493, offset(0.0001))
        assertThat(result.interestRecords[0].currency).isEqualTo(Currency.USD)
        assertThat(result.interestRecords[1].date).isEqualTo(LocalDate(2025, 12, 30))
    }

    @Test
    fun parsesSavingsEurStatement() {
        val csv = """
            Date,Description,"Value, EUR","Value, CZK",FX Rate,Price per share,Quantity of shares
            "Dec 31, 2025, 1:51:12 AM",Interest PAID EUR Class R IE000AZVL3K0,1.2345,30.5000,24.7000,,
        """.trimIndent()

        val result = RevolutParser.parseSavings(StringReader(csv))

        assertThat(result.interestRecords).hasSize(1)
        assertThat(result.interestRecords[0].currency).isEqualTo(Currency.EUR)
        assertThat(result.interestRecords[0].amount).isCloseTo(1.2345, offset(0.0001))
    }

    @Test
    fun parseSavingsFailsLoudlyOnUnrecognisedInterestRow() {
        // Localised statement (e.g. CZ): row begins with "Interest" but is not "PAID" / "Reinvested".
        val csv = """
            Date,Description,"Value, USD","Value, CZK",FX Rate,Price per share,Quantity of shares
            "Dec 31, 2025, 1:51:12 AM",Interest Accrued USD Class R IE000H9J0QX4,1.0000,21.0000,21.0000,,
        """.trimIndent()

        assertThatIllegalStateException()
            .isThrownBy { RevolutParser.parseSavings(StringReader(csv)) }
            .withMessageContaining("unrecognised Interest row")
            .withMessageContaining("Interest Accrued")
    }

    @Test
    fun parsesSavingsDateWithNarrowNoBreakSpaceBeforeAmPm() {
        // Real Revolut CSVs use U+202F (NARROW NO-BREAK SPACE) between time and AM/PM.
        val csv = "Date,Description,\"Value, USD\",\"Value, CZK\",FX Rate,Price per share,Quantity of shares\n" +
                "\"Dec 31, 2025, 1:21:00\u202FAM\",Interest PAID USD Class R IE000H9J0QX4,2.5000,52.0000,20.8000,,\n"

        val result = RevolutParser.parseSavings(StringReader(csv))

        assertThat(result.interestRecords).hasSize(1)
        assertThat(result.interestRecords[0].date).isEqualTo(LocalDate(2025, 12, 31))
        assertThat(result.interestRecords[0].amount).isCloseTo(2.5, offset(0.0001))
    }

}
