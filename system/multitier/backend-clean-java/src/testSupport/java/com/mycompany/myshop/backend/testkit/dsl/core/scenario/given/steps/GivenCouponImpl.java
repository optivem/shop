package com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.backend.testkit.common.Converter;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.testkit.dsl.port.given.steps.GivenCoupon;

public class GivenCouponImpl extends BaseGivenStep implements GivenCoupon {

    private String couponCode;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private Integer usageLimit;

    public GivenCouponImpl(GivenImpl given) {
        super(given);
        withCouponCode(ScenarioDefaults.DEFAULT_COUPON_CODE);
        withDiscountRate(ScenarioDefaults.DEFAULT_DISCOUNT_RATE);
        withValidFrom(ScenarioDefaults.EMPTY);
        withValidTo(ScenarioDefaults.EMPTY);
        withUsageLimit(ScenarioDefaults.DEFAULT_USAGE_LIMIT);
    }

    @Override
    public GivenCouponImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public GivenCouponImpl withDiscountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    @Override
    public GivenCouponImpl withDiscountRate(double discountRate) {
        return withDiscountRate(Converter.fromDouble(discountRate));
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
    public GivenCouponImpl withUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.myShop().publishCoupon()
            .couponCode(couponCode)
            .discountRate(discountRate)
            .validFrom(validFrom)
            .validTo(validTo)
            .usageLimit(usageLimit)
            .execute()
            .shouldSucceed();
    }
}
