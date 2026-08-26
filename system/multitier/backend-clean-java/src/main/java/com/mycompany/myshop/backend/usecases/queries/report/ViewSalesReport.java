package com.mycompany.myshop.backend.usecases.queries.report;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.report.ports.SalesReportReader;


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

    private final SalesReportReader salesReportReader;

    public ViewSalesReport(SalesReportReader salesReportReader) {
        this.salesReportReader = salesReportReader;
    }

    @Override
    public Result<ViewSalesReportResponse, UseCaseError> execute(ViewSalesReportRequest request) {
        var requested = request.topSkuLimit();
        if (requested != null && (requested < 1 || requested > MAX_TOP_SKU_LIMIT)) {
            return Result.err(new UseCaseError.Invalid(FIELD_TOP_SKU_LIMIT,
                    "Top SKU limit must be between 1 and " + MAX_TOP_SKU_LIMIT));
        }

        var limit = requested == null ? DEFAULT_TOP_SKU_LIMIT : requested;

        return Result.ok(new ViewSalesReportResponse(
                salesReportReader.revenueByCountryAndMonth(),
                salesReportReader.topSkusByRevenue(limit),
                salesReportReader.couponEffectiveness()));
    }
}
