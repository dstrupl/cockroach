package cz.solutions.cockroach

import org.joda.time.LocalDate

/**
 * Test-only constructors that supply reasonable defaults for fields most tests do not care about.
 * Production data classes intentionally do not declare defaults for these fields so that real
 * callers cannot silently drop broker/symbol/product/country attribution.
 */

fun dividendRecord(
    date: LocalDate,
    amount: Double,
    currency: Currency = Currency.USD,
    symbol: String = "TEST",
    broker: String = "TestBroker",
    country: String = "US",
): DividendRecord = DividendRecord(
    date = date,
    amount = amount,
    currency = currency,
    symbol = symbol,
    broker = broker,
    country = country,
)

fun interestRecord(
    date: LocalDate,
    amount: Double,
    currency: Currency = Currency.USD,
    product: String = "TestProduct",
    broker: String = "TestBroker",
    tax: Double = 0.0,
    country: String = "IE",
): InterestRecord = InterestRecord(
    date = date,
    amount = amount,
    currency = currency,
    product = product,
    broker = broker,
    tax = tax,
    country = country,
)
