package com.mycompany.myshop.backend.usecases.queries.coupon;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.common.PageSpec;
import com.mycompany.myshop.backend.usecases.queries.coupon.ports.CouponReader;

// A pure query: it copies primitives from the projection into the wire contract and builds no
// Coupon. That is not only cheaper -- Coupon's constructor rejects a discount rate of
// zero, so on the domain path one bad row failed the whole list.
//
// The projection is a record of its own rather than the response item itself, because
// REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE pins the wire contract to this package --
// and keeping it out of the query means renaming a response field does not edit a query string.
//
// The page and size are validated here for the reason given on
// com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistory: it is the use case that
// owns how much work one request may cost.
public class BrowseCoupons implements UseCase<BrowseCouponsRequest, BrowseCouponsResponse> {

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";

    private final CouponReader couponReader;

    public BrowseCoupons(CouponReader couponReader) {
        this.couponReader = couponReader;
    }

    @Override
    public Result<BrowseCouponsResponse, UseCaseError> execute(BrowseCouponsRequest request) {
        if (!PageSpec.isValidPage(request.page())) {
            return Result.err(new UseCaseError.Invalid(FIELD_PAGE,
                    "Page must be " + PageSpec.FIRST_PAGE + " or greater"));
        }
        if (!PageSpec.isValidSize(request.size())) {
            return Result.err(new UseCaseError.Invalid(FIELD_SIZE,
                    "Page size must be between 1 and " + PageSpec.MAX_SIZE));
        }

        var page = couponReader.listCoupons(
                new PageSpec(PageSpec.pageOrFirst(request.page()), PageSpec.sizeOrDefault(request.size())));

        return Result.ok(new BrowseCouponsResponse(
                page.items(), page.page(), page.size(), page.totalElements(), page.totalPages()));
    }
}
