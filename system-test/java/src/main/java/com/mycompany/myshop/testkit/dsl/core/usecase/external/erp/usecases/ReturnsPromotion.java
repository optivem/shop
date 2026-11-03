package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.dtos.ReturnsPromotionRequest;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;

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

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = ReturnsPromotionRequest.builder()
                .promotionActive(promotionActive)
                .discount(discount)
                .build();

        var result = driver.returnsPromotion(request);

        return new UseCaseResult<>(result.mapError(e -> SystemError.of(e.getMessage())), context, VoidVerification::new);
    }
}
