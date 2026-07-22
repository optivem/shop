package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.common.Converter;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class BrowseCouponsVerification extends ResponseVerification<BrowseCouponsResponse> {
    public BrowseCouponsVerification(BrowseCouponsResponse response, UseCaseContext context) {
        super(response, context);
    }

    public BrowseCouponsVerification hasCouponWithCode(String couponCodeAlias) {
        findCouponByCode(couponCodeAlias);
        return this;
    }

    public BrowseCouponsVerification couponHasDiscountRate(String couponCodeAlias, BigDecimal expectedDiscountRate) {
        var coupon = findCouponByCode(couponCodeAlias);
        assertThat(coupon.getDiscountRate())
                .as("Discount rate for coupon '%s'", couponCodeAlias)
                .isEqualByComparingTo(expectedDiscountRate);
        return this;
    }

    public BrowseCouponsVerification couponHasDiscountRate(String couponCodeAlias, double expectedDiscountRate) {
        return couponHasDiscountRate(couponCodeAlias, Converter.toBigDecimal(expectedDiscountRate));
    }

    public BrowseCouponsVerification couponHasDiscountRate(String couponCodeAlias, String expectedDiscountRate) {
        return couponHasDiscountRate(couponCodeAlias, Converter.toBigDecimal(expectedDiscountRate));
    }

    public BrowseCouponsVerification couponHasValidFrom(String couponCodeAlias, String expectedValidFrom) {
        var coupon = findCouponByCode(couponCodeAlias);
        var expectedInstant = Converter.toInstant(expectedValidFrom);
        assertThat(coupon.getValidFrom())
                .as("ValidFrom for coupon '%s'", couponCodeAlias)
                .isEqualTo(expectedInstant);
        return this;
    }

    public BrowseCouponsVerification couponHasValidTo(String couponCodeAlias, String expectedValidTo) {
        var coupon = findCouponByCode(couponCodeAlias);
        var expectedInstant = Converter.toInstant(expectedValidTo);
        assertThat(coupon.getValidTo())
                .as("ValidTo for coupon '%s'", couponCodeAlias)
                .isEqualTo(expectedInstant);
        return this;
    }

    public BrowseCouponsVerification couponHasUsageLimit(String couponCodeAlias, int expectedUsageLimit) {
        var coupon = findCouponByCode(couponCodeAlias);
        assertThat(coupon.getUsageLimit())
                .as("Usage limit for coupon '%s'", couponCodeAlias)
                .isEqualTo(expectedUsageLimit);
        return this;
    }

    public BrowseCouponsVerification couponHasUsedCount(String couponCode, int expectedUsedCount) {
        var coupon = findCouponByCode(couponCode);
        assertThat(coupon.getUsedCount())
                .as("Used count for coupon '%s'", couponCode)
                .isEqualTo(expectedUsedCount);
        return this;
    }

    private BrowseCouponsResponse.CouponDto findCouponByCode(String couponCodeAlias) {
        assertThat(couponCodeAlias)
                .as("Coupon code alias parameter")
                .isNotNull();

        assertThat(getResponse())
                .as("Response should not be null")
                .isNotNull();
        assertThat(getResponse().getCoupons())
                .as("Coupons list should not be null")
                .isNotNull();

        var couponCode = getContext().getParamValue(couponCodeAlias);

        return getResponse().getCoupons().stream()
                .filter(c -> couponCode.equals(c.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        String.format("Coupon with code '%s' not found. Available coupons: %s",
                                couponCode,
                                getResponse().getCoupons().stream()
                                        .map(BrowseCouponsResponse.CouponDto::getCode)
                                        .toList())));
    }
}
