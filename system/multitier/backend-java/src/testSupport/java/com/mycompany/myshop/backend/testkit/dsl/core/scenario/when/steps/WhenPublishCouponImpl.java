package com.mycompany.myshop.backend.support.core.scenario.when.steps;

import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResult;
import com.mycompany.myshop.backend.support.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.backend.support.core.scenario.ScenarioDefaults;
import com.mycompany.myshop.backend.support.core.shared.VoidVerification;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import com.mycompany.myshop.backend.support.port.when.steps.WhenPublishCoupon;
import java.math.BigDecimal;

public class WhenPublishCouponImpl extends BaseWhenStep<Void, VoidVerification>
        implements WhenPublishCoupon {

    private String couponCode = ScenarioDefaults.DEFAULT_COUPON_CODE;
    private String discountRate = ScenarioDefaults.DEFAULT_DISCOUNT_RATE;
    private String validFrom = ScenarioDefaults.EMPTY;
    private String validTo = ScenarioDefaults.EMPTY;
    private Integer usageLimit = ScenarioDefaults.DEFAULT_USAGE_LIMIT;

    public WhenPublishCouponImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    public WhenPublishCouponImpl withCouponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withDiscountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    @Override
    public WhenPublishCouponImpl withDiscountRate(double discountRate) {
        return withDiscountRate(BigDecimal.valueOf(discountRate).toPlainString());
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
    public WhenPublishCouponImpl withUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    protected ExecutionResult<Void, VoidVerification> execute(UseCaseDsl app) {
        var result = app.myShop().publishCoupon()
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
