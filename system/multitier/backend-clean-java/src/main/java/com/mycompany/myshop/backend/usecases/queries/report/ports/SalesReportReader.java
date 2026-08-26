package com.mycompany.myshop.backend.usecases.queries.report.ports;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportCouponEffectivenessResponse;import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportRevenueByCountryMonthResponse;import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportTopSkuResponse;

import java.util.List;

// The read side of the sales report: three questions the database answers by aggregating, and the
// application layer answered by loading every row and looping.
//
// Every method names the question, not the mechanism. There is no findAll here for a
// caller to fold over, which is the whole point -- an intent-named port leaves the adapter free to
// reply with one GROUP BY, and the loop has nowhere left to live.
//
// This is the port the demo is about: nothing it returns is a domain concept, and the domain
// never asks any of these questions. See the package javadoc for why it is not a domain repository.
public interface SalesReportReader {

    // Newest month first. Cancelled orders are excluded -- they are not revenue.
    List<ViewSalesReportRevenueByCountryMonthResponse> revenueByCountryAndMonth();

    // Highest revenue first, at most limit rows. The limit reaches the database, not a stream.
    List<ViewSalesReportTopSkuResponse> topSkusByRevenue(int limit);

    // One row per coupon, including coupons no order ever used.
    List<ViewSalesReportCouponEffectivenessResponse> couponEffectiveness();
}
