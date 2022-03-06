package com.cisco.td.general.cocroach;

import lombok.Value;

import java.util.List;
import java.util.Map;

import static com.cisco.td.general.cocroach.FormatingHelper.formatDouble;
import static com.cognitivesecurity.commons.util.Literals.map;

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
        return map(
                "dividendList", printableDividendList,
                "totalBruttoDollar", formatDouble(totalBruttoDollar),
                "totalTaxDollar", formatDouble(totalTaxDollar),
                "totalBruttoCrown", formatDouble(totalBruttoCrown),
                "totalTaxCrown", formatDouble(totalTaxCrown),
                "totalTaxReversal", totalTaxReversalDollar > 0 ? formatDouble(totalTaxReversalDollar) : "",
                "totalTaxReversalCrown", totalTaxReversalCrown > 0 ? formatDouble(totalTaxReversalCrown) : ""
        );
    }
}
