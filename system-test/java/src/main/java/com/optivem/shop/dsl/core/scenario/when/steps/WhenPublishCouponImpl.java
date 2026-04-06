package com.optivem.shop.dsl.core.scenario.when.steps;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.scenario.ExecutionResult;
import com.optivem.shop.dsl.core.scenario.ExecutionResultBuilder;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.core.shared.VoidVerification;
import com.optivem.shop.dsl.port.when.steps.WhenPublishCoupon;

public class WhenPublishCouponImpl extends BaseWhenStep<Void, VoidVerification> implements WhenPublishCoupon {
    private String code;
    private String discountRate;

    public WhenPublishCouponImpl(UseCaseDsl app) {
        super(app);
    }

    @Override
    public WhenPublishCouponImpl withCode(String code) {
        this.code = code;
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
    protected ExecutionResult<Void, VoidVerification> execute(UseCaseDsl app) {
        var result = app.shop().publishCoupon()
                .withCode(code)
                .withDiscountRate(discountRate)
                .execute();

        return new ExecutionResultBuilder<>(result).build();
    }
}
