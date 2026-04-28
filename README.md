# Cockroach will help you with your taxes

This small utility is for people using [Charles Schwab brokerage](https://www.schwab.com/) and/or
[E-Trade](https://www.etrade.com/) services in the [Czech Republic](https://en.wikipedia.org/wiki/Czech_Republic).

The program reads the Schwab JSON export of your stock transactions and optionally an E-Trade Gain and Loss CSV
export, then creates a summary of your sales and purchases for the tax year.

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


# Obtaining E-Trade Gain and Loss XLSX export

1.  Go to [Gains & Losses](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/gainsLosses).

2.  Select Year **2025**, Benefit type **All**, Capital Gain Status **All**, Covered Status **All**.
    ![](media/gains_losses_0.jpg)

3.  Click **Download** → **Download Expanded** to export the data as XLSX.
 
4.  Save the xlsx into directory called 'sales'

# Obtaining E-Trade ESPP confirmation PDFs

1.  Go to [Stock Plan Confirmations](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/stockPlanConfirmations).

2.  Select the relevant year and Benefit type: **ESPP**.
    ![](media/espp_confirm_0.jpg)

3.  Download all available confirmations.
 
4. Save the PDFs into directory called 'espp'

# Obtaining E-Trade RSU release confirmation PDFs

1.  Go to [Stock Plan Confirmations](https://us.etrade.com/etx/sp/stockplan?accountIndex=0&traxui=tsp_accountshome#/myAccount/stockPlanConfirmations).

2.  Select the relevant year and Benefit type: **Restricted stock units**.
    ![](media/rsu_confirm_0.jpg)

3.  Download all available confirmations.
 
4. Save the PDFs into directory called 'rsu'

# Obtaining E-Trade Dividends XLSX export

1.  Go to [Holdings](https://us.etrade.com/etx/sp/stockplan#/holdings).

2.  Scroll down to **Other Holdings**, expand it and then expand **Cash transactions**.
    Select the right date range. Don't tick "Dividends only" because you also want to see
    the withholding tax rows.
    ![](media/dividends_0.png)

3.  Download the report as **Excel**.

4.  Save the XLSX file into directory called 'dividends'

# Obtaining Interactive Brokers Dividend CSV export

1. Go to Transaction & History -> Transaction History
2. Select "Custom" and From Date and To Date
3. Click on "CSV" icon and download the file
4. Save the CSV file into directory called 'ib/dividends'

![](media/ib_transactions.png)


# Running the application

-   Compile and run
    cockroach/src/main/kotlin/cz/solutions/cockroach/CockroachMain.kt

-   it gets 3 required command line arguments - path to Schwab JSON export, year, and
    output dir. An optional 4th argument specifies the path to the E-Trade Gain and Loss CSV file.

-   it uses templates located here:
    cockroach/src/main/resources/cz/solutions/cockroach

-   the output are 4 simple .md files and an HTML guide

-   In InteliJ IDEA, you can convert the md files into pdf in Markdown
    export options under Tools \> Markdown Converter menu.\
    ![](media/image2.png)

# Compiling and Running

mvn clean install -am

mvn clean install shade:shade

java -jar target/cockroach-0.2-SNAPSHOT.jar /tmp/219114411.json 2025 /tmp/taxes

With E-Trade data:

java -jar target/cockroach-0.2-SNAPSHOT.jar /tmp/219114411.json 2025 /tmp/taxes /tmp/e-trade-dir

# Converting to PDF

-   pandoc sales_2021.md -V geometry:landscape
    \--pdf-engine=/Library/TeX/texbin/pdflatex -o sales.pdf

-   IDEA: Tools -\> Markdown Converter
