package com.optivem.shop.testkit.dsl.core.scenario.when.steps;

import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;
import com.optivem.shop.testkit.dsl.port.when.steps.WhenPublishCoupon;

import static com.optivem.shop.testkit.dsl.core.scenario.ScenarioDefaults.*;

public class WhenPublishCouponImpl extends BaseWhenStep<Void, VoidVerification> implements WhenPublishCoupon {
    private String couponCode;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private String usageLimit;

    public WhenPublishCouponImpl(UseCaseDsl app) {
        super(app);
        withCouponCode(DEFAULT_COUPON_CODE);
        withDiscountRate(DEFAULT_DISCOUNT_RATE);
    }

    @Override
    public WhenPublishCouponImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withDiscountRate(double discountRate) {
        return withDiscountRate(Converter.fromDouble(discountRate));
    }

    @Override
    public WhenPublishCouponImpl withDiscountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withValidFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withValidTo(String validTo) {
        this.validTo = validTo;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withUsageLimit(String usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withUsageLimit(int usageLimit) {
        return withUsageLimit(String.valueOf(usageLimit));
    }

    @Override
    protected ExecutionResult<Void, VoidVerification> execute(UseCaseDsl app) {
        var result = app.shop().publishCoupon()
                .couponCode(couponCode)
                .discountRate(discountRate)
                .validFrom(validFrom)
                .validTo(validTo)
                .usageLimit(usageLimit)
                .execute();

        return new ExecutionResultBuilder<>(result)
                .couponCode(couponCode)
                .build();
    }
}
