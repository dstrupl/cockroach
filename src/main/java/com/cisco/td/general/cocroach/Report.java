package com.cisco.td.general.cocroach;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Report {
    private final Template rsuTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("rsu.hbs");
    private final Template dividendTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("dividend.hbs");
    private final Template esppTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("espp.hbs");
    private final Template salesTemplate = new TemplateEngine(ReportGenerator.class, GeneralTemplateHelpers.class).load("sales.hbs");


    private final RsuReport rsuReport;
    private final DividendReport dividendReport;
    private final EsppReport esppReport;
    private final SalesReport salesReport;

    public String getRsu() {
        return rsuTemplate.render(rsuReport.asMap());
    }

    public String getDividend() {
        return dividendTemplate.render(dividendReport.asMap());
    }

    public String getEspp() {
        return esppTemplate.render(esppReport.asMap());
    }

    public String getSales() {
        return salesTemplate.render(salesReport.asMap());
    }

    public double taxToPay() {
        return (rsuReport.getRsuCroneValue() + salesReport.getProfitForTax() + esppReport.getProfitCroneValue() + dividendReport.getTotalBruttoCrown()) * 0.15 + dividendReport.getTotalTaxCrown();
    }

    public double taxableIncome() {
        return (rsuReport.getRsuCroneValue() + esppReport.getProfitCroneValue());
    }

    public double taxableDividendIncome() {
        return dividendReport.getTotalBruttoCrown();
    }

    public double payedDividendTax() {
        return dividendReport.getTotalTaxCrown();
    }

}
