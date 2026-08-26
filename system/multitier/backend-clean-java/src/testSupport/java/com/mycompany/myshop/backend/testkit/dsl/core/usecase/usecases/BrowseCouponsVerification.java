package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsItemResponse;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;

public class BrowseCouponsVerification extends ResponseVerification<BrowseCouponsResponse> {

    public BrowseCouponsVerification(BrowseCouponsResponse response) {
        super(response);
    }

    public BrowseCouponsVerification hasCouponWithCode(String couponCode) {
        findCouponByCode(couponCode);
        return this;
    }

    public BrowseCouponsVerification couponHasDiscountRate(
            String couponCode, String expectedDiscountRate) {
        assertThat(findCouponByCode(couponCode).discountRate())
            .as("discount rate of coupon '%s'", couponCode)
            .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public BrowseCouponsVerification couponHasUsageLimit(String couponCode, int expectedUsageLimit) {
        assertThat(findCouponByCode(couponCode).usageLimit())
            .as("usage limit of coupon '%s'", couponCode)
            .isEqualTo(expectedUsageLimit);
        return this;
    }

    public BrowseCouponsVerification couponHasUsedCount(String couponCode, int expectedUsedCount) {
        assertThat(findCouponByCode(couponCode).usedCount())
            .as("used count of coupon '%s'", couponCode)
            .isEqualTo(expectedUsedCount);
        return this;
    }

    private BrowseCouponsItemResponse findCouponByCode(String couponCode) {
        assertThat(getResponse().coupons()).as("coupons").isNotNull();

        return getResponse().coupons().stream()
            .filter(coupon -> couponCode.equals(coupon.code()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(String.format(
                "Coupon with code '%s' not found. Available coupons: %s",
                couponCode,
                getResponse().coupons().stream()
                    .map(BrowseCouponsItemResponse::code)
                    .toList())));
    }
}
