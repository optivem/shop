package com.optivem.shop.testkit.dsl.core.usecase.external.erp.usecases;

import com.optivem.shop.testkit.driver.port.external.erp.ErpDriver;
import com.optivem.shop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.optivem.shop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.optivem.shop.testkit.common.Converter;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;

public class ReturnsPromotion extends BaseErpUseCase<Void, VoidVerification> {
    private boolean promotionActive;
    private String discount;

    public ReturnsPromotion(ErpDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ReturnsPromotion withActive(boolean promotionActive) {
        this.promotionActive = promotionActive;
        return this;
    }

    public ReturnsPromotion withDiscount(String discount) {
        this.discount = discount;
        return this;
    }

    public ReturnsPromotion withDiscount(double discount) {
        return withDiscount(Converter.fromDouble(discount));
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = ReturnsPromotionRequest.builder()
                .promotionActive(promotionActive)
                .discount(discount)
                .build();

        var result = driver.returnsPromotion(request);

        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
