package cz.solutions.cockroach;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

import static cz.solutions.cockroach.FormatingHelper.formatRounded;

@RequiredArgsConstructor
public class Report {
    private final Template rsuTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("rsu.hbs");
    private final Template dividendTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("dividend.hbs");
    private final Template esppTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("espp.hbs");
    private final Template salesTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("sales.hbs");
    private final Template guideTemplate = new TemplateEngine(ReportGenerator.class, TemplateHelpers.class).load("guide.html.hbs");


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

    public String getGuide() {


        ImmutableMap<String, String> rsuAndEsppVars = ImmutableMap.of(
                "rsuProfitCroneValue", formatRounded(rsuReport.getRsuCroneValue()),
                "esppProfitCroneValue", formatRounded(esppReport.getProfitCroneValue()),
                "rsuAndEsppProfitCroneValue", formatRounded(rsuReport.getRsuCroneValue() + esppReport.getProfitCroneValue()),
                "rsuTaxableProfitCroneValue", formatRounded(rsuReport.getTaxableRsuCroneValue()),
                "esppTaxableProfitCroneValue", formatRounded(esppReport.getTaxableProfitCroneValue()),
                "rsuAndEsppTaxableProfitCroneValue", formatRounded(rsuReport.getTaxableRsuCroneValue() + esppReport.getTaxableProfitCroneValue())
        );
        ImmutableMap<String, String> salesVars = ImmutableMap.of(
                "taxableSellPriceCroneValue", formatRounded(salesReport.getSellCroneForTax()),
                "taxableBuyPriceCroneValue", formatRounded(salesReport.getBuyCroneForTax()),
                "taxableSellProfitCroneValue", formatRounded(salesReport.getProfitForTax())
        );
        ImmutableMap<String, String> dividentVars = ImmutableMap.of(
                "dividendCroneValue", formatRounded(dividendReport.getTotalBruttoCrown()),
                "dividendPayedTaxCroneValue", formatRounded(-dividendReport.getTotalTaxCrown())
        );

        ImmutableMap<String, String> variables = ImmutableMap.<String, String>builder()
                .putAll(rsuAndEsppVars)
                .putAll(salesVars)
                .putAll(dividentVars)
                .build();

        return render(
                guideTemplate,
                variables
        );
    }


    public double rsuAndEsppAndSalesProfitCroneVakue() {
        return rsuReport.getRsuCroneValue() + salesReport.getProfitForTax() + esppReport.getProfitCroneValue() ;
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
