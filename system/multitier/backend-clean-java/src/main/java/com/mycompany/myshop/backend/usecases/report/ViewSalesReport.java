package com.mycompany.myshop.backend.usecases.report;

import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.CouponEffectiveness;
import com.mycompany.myshop.backend.usecases.queries.RevenueByCountryMonth;
import com.mycompany.myshop.backend.usecases.queries.SalesReportQuery;
import com.mycompany.myshop.backend.usecases.queries.TopSkuByRevenue;

import java.util.List;

// A pure query, and the one that makes the theme's point most plainly: three aggregates and no
// domain object anywhere. Written the way the rest of the codebase reads, this use case would have
// pulled every order and every coupon into memory and folded them with streams -- the report is not
// a domain concept, so there was never an entity to ask.
//
// All this class does is validate a limit and copy primitives onto the wire contract. The
// aggregation is the adapter's, and that is the argument.
public class ViewSalesReport implements UseCase<ViewSalesReportRequest, ViewSalesReportResponse> {

    private static final String FIELD_TOP_SKU_LIMIT = "topSkuLimit";
    private static final int DEFAULT_TOP_SKU_LIMIT = 10;
    private static final int MAX_TOP_SKU_LIMIT = 100;

    private final SalesReportQuery salesReportQuery;

    public ViewSalesReport(SalesReportQuery salesReportQuery) {
        this.salesReportQuery = salesReportQuery;
    }

    @Override
    public Result<ViewSalesReportResponse, UseCaseError> execute(ViewSalesReportRequest request) {
        var requested = request.topSkuLimit();
        if (requested != null && (requested < 1 || requested > MAX_TOP_SKU_LIMIT)) {
            return Result.err(new UseCaseError.Invalid(FIELD_TOP_SKU_LIMIT,
                    "Top SKU limit must be between 1 and " + MAX_TOP_SKU_LIMIT));
        }

        var limit = requested == null ? DEFAULT_TOP_SKU_LIMIT : requested;

        var response = new ViewSalesReportResponse();
        response.setRevenueByCountryMonth(toRevenue(salesReportQuery.revenueByCountryAndMonth()));
        response.setTopSkus(toTopSkus(salesReportQuery.topSkusByRevenue(limit)));
        response.setCouponEffectiveness(toCoupons(salesReportQuery.couponEffectiveness()));
        return Result.ok(response);
    }

    private static List<ViewSalesReportResponse.RevenueByCountryMonthResponse> toRevenue(
            List<RevenueByCountryMonth> rows) {
        return rows.stream().map(row -> {
            var item = new ViewSalesReportResponse.RevenueByCountryMonthResponse();
            item.setCountry(row.country());
            item.setMonth(row.month());
            item.setOrderCount(row.orderCount());
            item.setQuantity(row.quantity());
            item.setSubtotalPrice(row.subtotalPrice());
            item.setTaxAmount(row.taxAmount());
            item.setTotalPrice(row.totalPrice());
            return item;
        }).toList();
    }

    private static List<ViewSalesReportResponse.TopSkuResponse> toTopSkus(List<TopSkuByRevenue> rows) {
        return rows.stream().map(row -> {
            var item = new ViewSalesReportResponse.TopSkuResponse();
            item.setSku(row.sku());
            item.setOrderCount(row.orderCount());
            item.setQuantity(row.quantity());
            item.setTotalPrice(row.totalPrice());
            return item;
        }).toList();
    }

    private static List<ViewSalesReportResponse.CouponEffectivenessResponse> toCoupons(
            List<CouponEffectiveness> rows) {
        return rows.stream().map(row -> {
            var item = new ViewSalesReportResponse.CouponEffectivenessResponse();
            item.setCode(row.code());
            item.setUsageLimit(row.usageLimit());
            item.setUsedCount(row.usedCount());
            item.setOrderCount(row.orderCount());
            item.setDiscountAmount(row.discountAmount());
            return item;
        }).toList();
    }
}
