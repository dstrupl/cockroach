# AGENTS.md — Project Conventions for AI Agents

This file is a quick orientation for any AI agent (Codex / Cursor / Claude / etc.)
working on this repository. Read it before making changes.

## What this project is

`cockroach` is a small **personal Czech-tax helper** for people who hold equity
or savings at:

- Charles Schwab (RSU/ESPP/dividends/sales — JSON export)
- E-Trade (RSU/ESPP/dividends/sales — XLSX/CSV/PDF exports)
- Degiro (dividends — XLS account statement)
- Revolut (dividends + savings interest — CSV statements)
- eToro (dividends — XLSX statement)
- VÚB (CZK savings interest — PDF statement)
- Interactive Brokers (dividends — CSV statement)

It produces:

- A PDF report per category (sales, RSU, ESPP, dividends, interest), in two
  variants (fixed yearly CNB rate, daily CNB rate).
- An HTML "guide" that walks through filling in the Czech tax return.

It is **not** a generally-supported product. It is a once-a-year personal tool
that the maintainer (`dstrupl`) runs against their own statements. Treat
breaking changes accordingly: small, well-tested, locally verifiable.

## Tech stack

- **Language:** Kotlin (`kotlin.version` is currently `2.3.20`)
- **JVM target:** Java 17
- **Build:** Maven (`pom.xml`)
- **Tests:** JUnit 5 (with `junit-vintage-engine` for legacy JUnit 4 tests)
- **Key libraries:**
  - Apache PDFBox — PDF text extraction (E-Trade RSU/ESPP, VÚB)
  - Apache POI — XLS/XLSX (E-Trade, Degiro). For eToro, custom regex parsing
    over the unzipped XLSX is used because POI complained about the file.
  - `org.apache.commons.csv` — Revolut, E-Trade gain/loss, Interactive Brokers
  - `joda-time` — all date math (do **not** introduce `java.time` casually,
    keep the codebase consistent)
  - `kotlinx.serialization` (JSON) + `com.charleskorn.kaml` (YAML) — config
  - Handlebars.java — HTML report templating

## Repository layout (high level)

```
src/main/kotlin/cz/solutions/cockroach/
  CockroachMain.kt           # entry point (positional args + YAML mode)
  CockroachConfig.kt         # YAML config schema (kaml)
  BrokerSource.kt            # interface every broker implements
  *BrokerSource.kt           # one per broker (SchwabBrokerSource, ETradeBrokerSource, ...)
  *Parser*.kt                # the actual parsing logic per broker / file type
  ParsedExport.kt            # data class aggregated across brokers
  *Record.kt                 # domain records (Rsu/Espp/Dividend/Tax/Sale/Interest)
  *Report*.kt                # report DTOs and HTML/PDF generators
  *ReportPreparation.kt      # currency conversion + summing
  CnbYearRatesSource.kt      # CNB rate fetcher (classpath + HTTP+cache)
  TabularExchangeRateProvider.kt  # FX lookup
src/main/resources/cz/solutions/cockroach/
  rates_*.txt                # bundled CNB rates per year
  *.hbs                      # Handlebars templates for HTML/PDF reports
src/test/kotlin/...          # JUnit 4/5 tests
input/                       # GIT-IGNORED. User puts statements + config.yaml here.
output/                      # GIT-IGNORED. Reports are written here.
```

## How to build / run

```bash
# Tests + jar
mvn clean install shade:shade

# Run with YAML config (preferred)
java -jar target/cockroach-0.3-SNAPSHOT.jar input/config.yaml

# Legacy positional args (Schwab + optional E-Trade and Interactive Brokers)
java -jar target/cockroach-0.3-SNAPSHOT.jar <schwab.json> <year> <outputDir> [etradeDir] [ibDir]
```

If only the Kotlin sources changed, `mvn -o test` is enough for the inner loop.

A few tests intentionally **skip** in CI because they read private statements
from the maintainer's machine (`assumeTrue(file.exists())` pattern, e.g.
`VubInterestPdfParserTest` and `EsppPdfParserTest`). That is by design — do not "fix" them by
checking in real statements.

## Coding conventions and gotchas

### Style
- Kotlin idiomatic where possible; data classes for records; `object` for
  stateless helpers.
- Joda `LocalDate` everywhere — match existing code.
- Keep `Currency` enum coverage in sync when you add a new ISO currency
  (search for `enum class Currency`).
- Numbers are `Double` throughout. Don't switch to `BigDecimal` casually —
  reports compare against existing pre-shipped expected output.

### Per-broker parsers
Each broker has:
1. `XxxBrokerSource : BrokerSource` (entry point, owns input files)
2. one or more parsers
3. tests in `src/test/...` with redacted real-world fixtures under
   `src/test/resources`

When adding a new broker, follow that triplet. Add it to:
- `CockroachConfig` (new optional field)
- `CockroachMain.runCockroach` YAML branch (instantiate the source)
- `README.md` (how to obtain the export, where to drop it)

### FX conversion
- `TabularExchangeRateProvider` returns CZK-per-foreign for a given date.
- For brokers that mix currencies on a single statement (Degiro), use the
  **value date** (not booking/trade date) for FX.
- Foreign-source interest goes on **Příloha č. 3** of the Czech tax return,
  including VÚB interest sourced from Slovakia even when paid in CZK. Only
  Czech-source CZK interest belongs on the konečné zdanění page. Keep that
  distinction in `InterestReportPreparation`.

### Withholding tax (WHT)
- Schwab and E-Trade statements report WHT explicitly → use as-is.
- Degiro statements report WHT explicitly when it applies; the parser emits an
  explicit zero tax record when no withholding row exists.
- Revolut **does not** show WHT. The parser performs a *gross-up*
  (`gross = net / (1 - whtRate)`) using a configurable `revolut.whtRate`
  (default 0.15, US/CZ treaty). If you change this default, make sure the
  README and the `RevolutParser.DEFAULT_WHT_RATE` constant agree.
- eToro reports net dividend and WHT separately; the parser uses `gross = net +
  WHT` but assumes US source for every dividend. Non-US-looking tickers produce
  a warning and still require manual verification.
- Interactive Brokers reports WHT explicitly. Initial support intentionally
  accepts only USD-base-currency, US-source dividend exports and fails loudly
  on unsupported layouts or non-US-looking tickers.

### CNB rates
- Bundled in `src/main/resources/.../rates_<year>.txt`.
- `ClasspathOrHttpCnbYearRatesSource` prefers bundled snapshots, keeping past
  tax years reproducible and available offline.
- For an unbundled incomplete year (current year, or year+1 within
  `safeDaysAfterYearEnd=30`), `HttpCnbYearRatesSource` downloads fresh from
  `cnb.cz` and **does not cache**.
- Complete unbundled years cache under `~/.cache/cockroach/rates/`.
- HTTP downloads use connect/read timeouts and validate the CNB header before
  using or caching the response.

### What you may NOT casually change

- The PDF/HTML report layouts and the totals math — they are scrutinized
  against the actual Czech tax form once a year. If you must change a number
  shown in a report, attach reasoning + a regression test.
- The bundled `rates_*.txt` files — do not "reformat" them; their column
  layout is the CNB native format and the parser is sensitive to it.

## Known issues / good first cleanups

Keep follow-up changes small and focused.

- `RevolutParser` savings parsing recognizes only English `Interest PAID`.
  Unknown/localized interest rows fail loudly and must be re-exported in
  English or supported explicitly.
- `EtoroXlsxParser` uses regex over the unzipped XML with hardcoded column
  letters — fragile against eToro layout tweaks; brittle but tested against
  the maintainer's recent statement.
- `VubInterestPdfParser` validates the configured year against the statement's
  closing-balance year, but custom statements spanning two calendar years are
  unsupported because individual posting rows contain only `DD/MM`.
- `DividentReportPreparation` (the `Divident` typo is retained intentionally)
  pairs rows by `(broker, symbol, date)` and requires an explicit tax row,
  including a zero record for genuine 0% withholding.
- Handlebars 4.4.0 has a high-severity path-traversal advisory fixed in 4.5.2.
  Current template names are hardcoded, limiting exposure, but the dependency
  should be upgraded in a focused follow-up.

## When in doubt

- Check the README for end-user-visible behavior.
- Re-run `mvn test` before committing; expect a few skipped tests on a fresh
  checkout (they require private statements on disk).
- Open a draft PR on `dstrupl/cockroach` — the `dstrupl` GitHub identity is
  the one that owns and reviews this repo.
