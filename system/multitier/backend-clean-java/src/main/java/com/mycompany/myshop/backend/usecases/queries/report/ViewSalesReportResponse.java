package com.mycompany.myshop.backend.usecases.queries.report;


import java.util.List;

// The sales report as it goes on the wire: the three projections the port returned, unmodified.
//
// This is the one read that needs an envelope for a reason other than paging -- it composes three
// separate port calls, so there is no single projection to hand back. What is inside it is still
// the projections themselves. See
// com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse for why a read
// side does not copy a row on its way out.
public record ViewSalesReportResponse(
        List<ViewSalesReportRevenueByCountryMonthResponse> revenueByCountryMonth,
        List<ViewSalesReportTopSkuResponse> topSkus,
        List<ViewSalesReportCouponEffectivenessResponse> couponEffectiveness) { }
