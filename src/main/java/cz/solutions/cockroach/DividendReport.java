package cz.solutions.cockroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class DividendReport {
    List<PrintableDividend> printableDividendList;
    double totalBruttoDollar;
    double totalTaxDollar;
    double totalBruttoCrown;
    double totalTaxCrown;
    double totalTaxReversalDollar;
    double totalTaxReversalCrown;

    public Map<String,?> asMap(){
        return Map.of(
                "dividendList", printableDividendList,
                "totalBruttoDollar", FormatingHelper.formatDouble(totalBruttoDollar),
                "totalTaxDollar", FormatingHelper.formatDouble(totalTaxDollar),
                "totalBruttoCrown", FormatingHelper.formatDouble(totalBruttoCrown),
                "totalTaxCrown", FormatingHelper.formatDouble(totalTaxCrown),
                "totalTaxReversal", totalTaxReversalDollar > 0 ? FormatingHelper.formatDouble(totalTaxReversalDollar) : "",
                "totalTaxReversalCrown", totalTaxReversalCrown > 0 ? FormatingHelper.formatDouble(totalTaxReversalCrown) : ""
        );
    }
}
