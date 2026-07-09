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

fun taxRecord(
    date: LocalDate,
    amount: Double,
    currency: Currency = Currency.USD,
    symbol: String = "TEST",
    broker: String = "TestBroker",
): TaxRecord = TaxRecord(
    date = date,
    amount = amount,
    currency = currency,
    symbol = symbol,
    broker = broker,
)

fun taxReversalRecord(
    date: LocalDate,
    amount: Double,
    currency: Currency = Currency.USD,
    symbol: String = "TEST",
    broker: String = "TestBroker",
): TaxReversalRecord = TaxReversalRecord(
    date = date,
    amount = amount,
    currency = currency,
    symbol = symbol,
    broker = broker,
)

fun saleRecord(
    date: LocalDate,
    type: String,
    quantity: Double,
    salePrice: Double,
    purchasePrice: Double,
    purchaseFmv: Double,
    purchaseDate: LocalDate,
    grantId: String?,
    symbol: String = "TEST",
    broker: String = "TestBroker",
): SaleRecord = SaleRecord(
    date = date,
    type = type,
    quantity = quantity,
    salePrice = salePrice,
    purchasePrice = purchasePrice,
    purchaseFmv = purchaseFmv,
    purchaseDate = purchaseDate,
    grantId = grantId,
    symbol = symbol,
    broker = broker,
)

fun rsuRecord(
    date: LocalDate,
    quantity: Int,
    vestFmv: Double,
    vestDate: LocalDate,
    grantId: String,
    symbol: String = "TEST",
    broker: String = "TestBroker",
): RsuRecord = RsuRecord(
    date = date,
    quantity = quantity,
    vestFmv = vestFmv,
    vestDate = vestDate,
    grantId = grantId,
    symbol = symbol,
    broker = broker,
)

fun esppRecord(
    date: LocalDate,
    quantity: Double,
    purchasePrice: Double,
    subscriptionFmv: Double,
    purchaseFmv: Double,
    purchaseDate: LocalDate,
    symbol: String = "TEST",
    broker: String = "TestBroker",
): EsppRecord = EsppRecord(
    date = date,
    quantity = quantity,
    purchasePrice = purchasePrice,
    subscriptionFmv = subscriptionFmv,
    purchaseFmv = purchaseFmv,
    purchaseDate = purchaseDate,
    symbol = symbol,
    broker = broker,
)
