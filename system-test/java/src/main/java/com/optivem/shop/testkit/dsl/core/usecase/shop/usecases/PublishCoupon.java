package com.optivem.shop.testkit.dsl.core.usecase.shop.usecases;

import com.optivem.shop.testkit.driver.port.shop.dtos.PublishCouponRequest;
import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;

public class PublishCoupon extends BaseShopUseCase<Void, VoidVerification> {
    private String couponCodeParamAlias;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private String usageLimit;

    public PublishCoupon(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public PublishCoupon couponCode(String couponCodeParamAlias) {
        this.couponCodeParamAlias = couponCodeParamAlias;
        return this;
    }

    public PublishCoupon discountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    public PublishCoupon validFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public PublishCoupon validTo(String validTo) {
        this.validTo = validTo;
        return this;
    }

    public PublishCoupon usageLimit(String usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var couponCode = context.getParamValue(couponCodeParamAlias);

        var request = PublishCouponRequest.builder()
                .code(couponCode)
                .discountRate(discountRate)
                .validFrom(validFrom)
                .validTo(validTo)
                .usageLimit(usageLimit)
                .build();

        var result = driver.publishCoupon(request);
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
