package com.optivem.shop.dsl.core.usecase.shop.usecases;

import com.optivem.shop.dsl.common.Converter;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.core.shared.UseCaseResult;
import com.optivem.shop.dsl.core.shared.VoidVerification;
import com.optivem.shop.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.shop.dsl.driver.port.shop.ShopDriver;
import com.optivem.shop.dsl.driver.port.shop.dtos.PublishCouponRequest;

public class PublishCoupon extends BaseShopUseCase<Void, VoidVerification> {
    private String code;
    private String discountRate;

    public PublishCoupon(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public PublishCoupon withCode(String code) {
        this.code = code;
        return this;
    }

    public PublishCoupon withDiscountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    public PublishCoupon withDiscountRate(double discountRate) {
        return withDiscountRate(Converter.fromDouble(discountRate));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = PublishCouponRequest.builder()
                .code(code)
                .discountRate(discountRate)
                .build();

        var result = driver.publishCoupon(request);
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
