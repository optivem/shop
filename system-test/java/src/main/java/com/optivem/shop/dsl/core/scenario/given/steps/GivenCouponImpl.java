package com.optivem.shop.dsl.core.scenario.given.steps;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.scenario.given.GivenImpl;
import com.optivem.shop.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.dsl.port.given.steps.GivenCoupon;

public class GivenCouponImpl extends BaseGivenStep implements GivenCoupon {
    private String code;
    private String discountRate;

    public GivenCouponImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenCouponImpl withCode(String code) {
        this.code = code;
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
    public void execute(UseCaseDsl app) {
        app.shop().publishCoupon()
                .withCode(code)
                .withDiscountRate(discountRate)
                .execute()
                .shouldSucceed();
    }
}
