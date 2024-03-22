package cz.solutions.cockroach;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class Report {
    private final Template rsuTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("rsu.hbs");
    private final Template dividendTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("dividend.hbs");
    private final Template esppTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("espp.hbs");
    private final Template salesTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("sales.hbs");


    private final RsuReport rsuReport;
    private final DividendReport dividendReport;
    private final EsppReport esppReport;
    private final SalesReport salesReport;

    public String getRsu() {
        return render(rsuTemplate, rsuReport.asMap());
    }

    public String getDividend() {
        return render(dividendTemplate, dividendReport.asMap());
    }

    public String getEspp() {
        return render(esppTemplate, esppReport.asMap());
    }

    public String getSales() {
        return render(salesTemplate, salesReport.asMap());
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

    public String render(Template template, Map<String, ?> variables) {
        try {
            Context context = Context.newBuilder(variables)
                    .resolver(
                            MapValueResolver.INSTANCE,
                            JavaBeanValueResolver.INSTANCE,
                            FieldValueResolver.INSTANCE
                    )
                    .build();
            return template.apply(context);
        } catch (IOException e) {
            throw new RuntimeException("Could not render template", e);
        }
    }


}
