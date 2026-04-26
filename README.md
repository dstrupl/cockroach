# Cockroach will help you with your taxes

This small utility is for people using [Charles Schwab brokerage](https://www.schwab.com/),
[E-Trade](https://www.etrade.com/), [Degiro](https://www.degiro.com/) and/or
[Revolut](https://www.revolut.com/) services in the
[Czech Republic](https://en.wikipedia.org/wiki/Czech_Republic).

The program reads the Schwab JSON export of your stock transactions, optionally an E-Trade Gain and Loss
XLSX/CSV export, optionally a Degiro account statement (`.xls`), and optionally Revolut Stocks and
Flexible Cash Funds CSV statements, then creates a summary of your sales, purchases and dividends for
the tax year.

All input files referenced in this README (and the YAML config) are assumed to live under the
`input/` folder at the repository root (which is git-ignored). See the [Input layout](#input-layout)
section below for the exact directory structure.

# Obtaining Schwab CSV export for last year

1.  Go to "Transaction History" tab.

2.  Choose "Equity Award Center" from the dropdown.

3.  Chose "Custom" Data range and interval from the last year (with
    overlap)
    -   Some events (e.g. ESPP) are reported with delay, so overlap of
        several days is necessary.
    -   Alternatively, you can export everything.
    - ![](media/image0.png)

4.  Click "Export"

5.  Select "JSON" radio button, click "Export"
![](media/image1.png)

6.  Save the JSON file into `input/`.


# Obtaining E-Trade Gain and Loss XLSX export

1.  Go to [Gains & Losses](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/gainsLosses).

2.  Select Year **2025**, Benefit type **All**, Capital Gain Status **All**, Covered Status **All**.
    ![](media/gains_losses_0.jpg)

3.  Click **Download** → **Download Expanded** to export the data as XLSX.

4.  Save the xlsx into `input/etrade/sales/`.

# Obtaining E-Trade ESPP confirmation PDFs

1.  Go to [Stock Plan Confirmations](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/stockPlanConfirmations).

2.  Select the relevant year and Benefit type: **ESPP**.
    ![](media/espp_confirm_0.jpg)

3.  Download all available confirmations.

4.  Save the PDFs into `input/etrade/espp/`.

# Obtaining E-Trade RSU release confirmation PDFs

1.  Go to [Stock Plan Confirmations](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/stockPlanConfirmations).

2.  Select the relevant year and Benefit type: **Restricted stock units**.
    ![](media/rsu_confirm_0.jpg)

3.  Download all available confirmations.

4.  Save the PDFs into `input/etrade/rsu/`.

# Obtaining E-Trade Dividends XLSX export

1.  Go to [Holdings](https://us.etrade.com/etx/sp/stockplan#/holdings).

2.  Scroll down to **Other Holdings**, expand it and then expand **Cash transactions**.
    Select the right date range. Don't tick "Dividends only" because you also want to see
    the withholding tax rows.
    ![](media/dividends_0.png)

3.  Download the report as **Excel**.

4.  Save the XLSX file into `input/etrade/dividends/`.

# Obtaining Degiro account statement

1.  Log in to [Degiro Account Overview](https://trader.degiro.nl/trader/#/account-overview) and open **Inbox → Account Statement**
    (Czech: *Inbox → Přehled účtu*).

2.  Set the date range to cover the relevant tax year (overlap of a few days at both ends is
    safe; only rows whose **value date** (`Datum valuty`) falls inside the requested year are
    used for the report).
    ![](media/degiro.png)
3.  Export as **XLS**.

4.  Save the file into `input/` (e.g. `input/Accounts_Degiro_2025.xls`).

Notes:
- Only `Dividenda` and `Daň z dividendy` rows are used.
- `ADR/GDR Pass-Through poplatek` rows are custody fees (not withholding tax) and are
  ignored; the total amount that was skipped is logged on stdout for transparency.
- All currencies present in the file (USD, EUR, CZK, ...) are handled; the daily CNB rate
  for the value date is used for FX conversion.

# Obtaining Revolut statements

Revolut does not expose a public API for personal accounts, so the CSV exports from the app
are the only data source. There are two relevant statements:

## Revolut Stocks (dividends)

1.  In the Revolut app, open **Stocks → ⋯ (More) → Statement**.

2.  Choose **Excel (CSV)** format and the relevant date range (the tax year, with a few days
    of overlap is safe).

3.  Save the file into `input/revolut/` (e.g.
    `input/revolut/trading-account-statement_2025-01-01_2025-12-31_en-us_<hash>.csv`).

Notes:
- Only `DIVIDEND` rows are processed; `BUY`, `SELL`, `CASH WITHDRAWAL` are ignored.
- **Withholding tax is not reported on the statement.** Revolut deducts US WHT at source and
  reports only the *net* amount that landed in your account. The parser therefore performs a
  mathematical *gross-up*: `gross = net / (1 - whtRate)` (default `whtRate = 0.15`, the US/CZ
  treaty rate when a W-8BEN is on file, which Revolut signs for you automatically). The
  computed WHT is emitted as a tax credit so your CZ tax return matches what was actually
  withheld. If your treaty rate differs, override `revolut.whtRate` in the YAML.
- `DIVIDEND TAX (CORRECTION)` rows always come in cancelling pairs (a debit and an immediate
  credit of the same magnitude); they are summed and ignored, with a log line confirming the
  net is zero.

## Revolut Flexible Cash Funds (interest)

1.  In the Revolut app, open **Savings → Flexible Cash Funds → Statement**.

2.  Choose **Excel (CSV)** format and the relevant date range. Export *one statement per
    currency* (e.g. one for the USD fund and one for the EUR fund).

3.  Save the files into `input/revolut/` (e.g.
    `input/revolut/savings-statement_2025-01-01_2025-12-31_en-us_<hash>.csv`).

Notes:
- Only `Interest PAID` rows are taken as gross §8 *interest on non-equity securities*
  income. `Interest Reinvested`, `BUY`, and `SELL` rows are ignored (no §10 capital-gain
  calculation is performed).
- `Service Fee Charged` rows are logged for transparency only — per Revolut's CZ tax
  guidance these fees are *not* deductible from the §8 interest base.
- Each statement is a single-currency file; the currency is auto-detected from the
  `Value, <CCY>` column header.

# Input layout

All inputs (broker exports and the YAML config) live under `input/`. A typical layout is:

```
input/
├── config.yaml
├── schwab-export.json              # Schwab JSON export
├── Accounts_Degiro.xls             # Degiro account statement
├── BenefitHistory.xlsx             # E-Trade Benefit History export (RSU + ESPP, optional alternative to etrade/rsu + etrade/espp)
├── etrade/                         # E-Trade data directory
│   ├── rsu/         *.pdf          # RSU release confirmations (skipped when etradeBenefitHistory is configured)
│   ├── espp/        *.pdf          # ESPP purchase confirmations (skipped when etradeBenefitHistory is configured)
│   ├── dividends/   *.xlsx         # single dividends export
│   └── sales/       *.xlsx         # single Gain & Loss export
└── revolut/                        # Revolut CSV statements
    ├── trading-account-statement_*.csv   # Stocks (dividends)
    └── savings-statement_*.csv           # Flexible Cash Funds (one per currency)
```

Each broker is optional; include only what applies to you.

# Running the application

There are two ways to invoke the tool:

## YAML config (recommended for multi-broker setups)

Create `input/config.yaml` describing the inputs and run with a single argument:

```yaml
year: 2025
outputDir: ./output
schwab: ./input/schwab-export.json    # optional
etrade: ./input/etrade                # optional, layout shown above
etradeBenefitHistory: ./input/BenefitHistory.xlsx   # optional; alternative to etrade/rsu + etrade/espp
degiro:                               # optional, list of Degiro .xls files
  - ./input/Accounts_Degiro_2025.xls
revolut:                              # optional Revolut block
  whtRate: 0.15                       # US/CZ treaty rate; override only if yours differs
  stocks:                             # list of Revolut Stocks CSV statements
    - ./input/revolut/trading-account-statement_2024-01-01_2024-12-31_en-us_xxxxxx.csv
  savings:                            # list of Flexible Cash Funds CSV statements (one per currency)
    - ./input/revolut/savings-statement_2024-01-01_2024-12-31_en-us_USD_xxxxxx.csv
    - ./input/revolut/savings-statement_2024-01-01_2024-12-31_en-us_EUR_xxxxxx.csv
```

```
java -jar target/cockroach-0.3-SNAPSHOT.jar input/config.yaml
```

At least one of `schwab`, `etrade`, `degiro`, `revolut.stocks`, `revolut.savings` must be present.

## Positional arguments (legacy, Schwab + E-Trade only)

-   Compile and run
    cockroach/src/main/kotlin/cz/solutions/cockroach/CockroachMain.kt

-   it gets 3 required command line arguments - path to Schwab JSON export, year, and
    output dir. An optional 4th argument specifies the path to the E-Trade data directory.

-   it uses templates located here:
    cockroach/src/main/resources/cz/solutions/cockroach

-   the output are PDF reports and an HTML guide for both fixed and dynamic exchange-rate
    variants.

-   In InteliJ IDEA, you can convert the md files into pdf in Markdown
    export options under Tools \> Markdown Converter menu.\
    ![](media/image2.png)

# Compiling and Running

mvn clean install -am

mvn clean install shade:shade

java -jar target/cockroach-0.3-SNAPSHOT.jar input/schwab-export.json 2025 ./output

With E-Trade data:

java -jar target/cockroach-0.3-SNAPSHOT.jar input/schwab-export.json 2025 ./output input/etrade

With YAML config (Schwab and/or E-Trade and/or Degiro and/or Revolut):

java -jar target/cockroach-0.3-SNAPSHOT.jar input/config.yaml

# Converting to PDF

-   pandoc sales_2021.md -V geometry:landscape
    \--pdf-engine=/Library/TeX/texbin/pdflatex -o sales.pdf

-   IDEA: Tools -\> Markdown Converter
