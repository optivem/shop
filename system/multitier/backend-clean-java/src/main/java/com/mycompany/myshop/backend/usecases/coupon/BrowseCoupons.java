package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.queries.CouponListItem;
import com.mycompany.myshop.backend.usecases.queries.CouponQuery;

/**
 * A pure query: it copies primitives from the projection into the wire contract and builds no
 * {@code Coupon}. That is not only cheaper -- {@code Coupon}'s constructor rejects a discount rate of
 * zero, so on the domain path one bad row failed the whole list.
 *
 * <p>The projection is a record of its own rather than the response item itself, because
 * {@code REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE} pins the wire contract to this package --
 * and keeping it out of the query means renaming a response field does not edit a query string.
 */
public class BrowseCoupons implements UseCase<BrowseCouponsRequest, BrowseCouponsResponse> {

    private final CouponQuery couponQuery;

    public BrowseCoupons(CouponQuery couponQuery) {
        this.couponQuery = couponQuery;
    }

    @Override
    public Result<BrowseCouponsResponse, UseCaseError> execute(BrowseCouponsRequest request) {
        var items = couponQuery.listCoupons().stream()
                .map(BrowseCoupons::toItem)
                .toList();

        var response = new BrowseCouponsResponse();
        response.setCoupons(items);
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
