package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.CouponListItem;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;
import com.mycompany.myshop.backend.usecases.queries.PageSpec;

// A pure query: it copies primitives from the projection into the wire contract and builds no
// Coupon. That is not only cheaper -- Coupon's constructor rejects a discount rate of
// zero, so on the domain path one bad row failed the whole list.
//
// The projection is a record of its own rather than the response item itself, because
// REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE pins the wire contract to this package --
// and keeping it out of the query means renaming a response field does not edit a query string.
//
// The page size is validated here for the reason given on
// com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory: it is the use case that
// owns how much work one request may cost.
public class BrowseCoupons implements UseCase<BrowseCouponsRequest, BrowseCouponsResponse> {

    private static final String FIELD_SIZE = "size";

    private final CouponQuery couponQuery;

    public BrowseCoupons(CouponQuery couponQuery) {
        this.couponQuery = couponQuery;
    }

    @Override
    public Result<BrowseCouponsResponse, UseCaseError> execute(BrowseCouponsRequest request) {
        if (!PageSpec.isValidSize(request.size())) {
            return Result.err(new UseCaseError.Invalid(FIELD_SIZE,
                    "Page size must be between 1 and " + PageSpec.MAX_SIZE));
        }

        var page = couponQuery.listCoupons(
                new PageSpec<>(PageSpec.sizeOrDefault(request.size()), request.cursor()));

        var response = new BrowseCouponsResponse();
        response.setCoupons(page.items().stream().map(BrowseCoupons::toItem).toList());
        response.setHasMore(page.hasMore());
        response.setNextCursor(page.hasMore()
                ? page.last().map(CouponListItem::code).orElse(null)
                : null);
        return Result.ok(response);
    }

    private static BrowseCouponsResponse.BrowseCouponsItemResponse toItem(CouponListItem coupon) {
        var item = new BrowseCouponsResponse.BrowseCouponsItemResponse();
        item.setCode(coupon.code());
        item.setDiscountRate(coupon.discountRate());
        item.setValidFrom(coupon.validFrom());
        item.setValidTo(coupon.validTo());
        item.setUsageLimit(coupon.usageLimit());
        item.setUsedCount(coupon.usedCount());
        return item;
    }
}
