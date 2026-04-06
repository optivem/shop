package com.optivem.shop.dsl.core.usecase.shop.usecases;

import com.optivem.shop.dsl.core.shared.ResponseVerification;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.driver.port.shop.dtos.BrowseCouponsResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowseCouponsVerification extends ResponseVerification<BrowseCouponsResponse> {
    public BrowseCouponsVerification(BrowseCouponsResponse response, UseCaseContext context) {
        super(response, context);
    }

    public BrowseCouponsVerification containsCouponWithCode(String expectedCode) {
        var coupons = getResponse().getCoupons();
        assertThat(coupons)
                .withFailMessage("Expected coupon with code '%s' to be present, but was not found", expectedCode)
                .anyMatch(c -> expectedCode.equals(c.getCode()));
        return this;
    }

    public BrowseCouponsVerification couponCount(int expectedCount) {
        var actualCount = getResponse().getCoupons().size();
        assertThat(actualCount)
                .withFailMessage("Expected %d coupons, but found %d", expectedCount, actualCount)
                .isEqualTo(expectedCount);
        return this;
    }
}
