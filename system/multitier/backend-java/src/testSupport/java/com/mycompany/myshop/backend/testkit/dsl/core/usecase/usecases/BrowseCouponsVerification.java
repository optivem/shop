package com.mycompany.myshop.backend.support.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;

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
        assertThat(findCouponByCode(couponCode).getDiscountRate())
            .as("discount rate of coupon '%s'", couponCode)
            .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public BrowseCouponsVerification couponHasUsageLimit(String couponCode, int expectedUsageLimit) {
        assertThat(findCouponByCode(couponCode).getUsageLimit())
            .as("usage limit of coupon '%s'", couponCode)
            .isEqualTo(expectedUsageLimit);
        return this;
    }

    public BrowseCouponsVerification couponHasUsedCount(String couponCode, int expectedUsedCount) {
        assertThat(findCouponByCode(couponCode).getUsedCount())
            .as("used count of coupon '%s'", couponCode)
            .isEqualTo(expectedUsedCount);
        return this;
    }

    private BrowseCouponsResponse.BrowseCouponsItemResponse findCouponByCode(String couponCode) {
        assertThat(getResponse().getCoupons()).as("coupons").isNotNull();

        return getResponse().getCoupons().stream()
            .filter(coupon -> couponCode.equals(coupon.getCode()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(String.format(
                "Coupon with code '%s' not found. Available coupons: %s",
                couponCode,
                getResponse().getCoupons().stream()
                    .map(BrowseCouponsResponse.BrowseCouponsItemResponse::getCode)
                    .toList())));
    }
}
