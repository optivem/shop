package com.mycompany.myshop.backend.usecases.queries.report;

import java.math.BigDecimal;

// One SKU's contribution to revenue. Same rule as ViewSalesReportRevenueByCountryMonthResponse: no value objects,
// no Guard.
//
// There is no Product here either. The domain has one, but it holds a SKU and a price and
// knows nothing about what was sold -- so the report would have to load every order anyway to build
// the number, which is the loop this replaces.
public record ViewSalesReportTopSkuResponse(
        String sku,
        long orderCount,
        long quantity,
        BigDecimal totalPrice) { }
