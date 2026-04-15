package com.optivem.shop.testkit.dsl.core.scenario.given.steps;

import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.port.given.steps.GivenCoupon;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class GivenCouponImpl extends BaseGivenStep implements GivenCoupon {
    private String couponCode;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private String usageLimit;

    public GivenCouponImpl(GivenImpl given) {
        super(given);

        withCouponCode(DEFAULT_COUPON_CODE);
        withDiscountRate(DEFAULT_DISCOUNT_RATE);
        withValidFrom(EMPTY);
        withValidTo(EMPTY);
        withUsageLimit(EMPTY);
    }

    @Override
    public GivenCouponImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public GivenCouponImpl withDiscountRate(double discountRate) {
        return withDiscountRate(Converter.fromDouble(discountRate));
    }

    @Override
    public GivenCouponImpl withDiscountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    @Override
    public GivenCouponImpl withValidFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    @Override
    public GivenCouponImpl withValidTo(String validTo) {
        this.validTo = validTo;
        return this;
    }

    @Override
    public GivenCouponImpl withUsageLimit(String usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    public GivenCouponImpl withUsageLimit(int usageLimit) {
        return withUsageLimit(Converter.fromInteger(usageLimit));
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.shop().publishCoupon()
                .couponCode(couponCode)
                .discountRate(discountRate)
                .validFrom(validFrom)
                .validTo(validTo)
                .usageLimit(usageLimit)
                .execute()
                .shouldSucceed();
    }
}
