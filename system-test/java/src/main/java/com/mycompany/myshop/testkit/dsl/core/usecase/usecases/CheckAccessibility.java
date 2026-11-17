package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckAccessibilityRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckAccessibilityResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

/** Runs axe on the page the last step left open (FT Step 4). UI channel only. */
public class CheckAccessibility extends BaseMyShopUseCase<CheckAccessibilityResponse, CheckAccessibilityVerification> {
    public CheckAccessibility(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<CheckAccessibilityResponse, CheckAccessibilityVerification> execute() {
        var result = driver.checkAccessibility(CheckAccessibilityRequest.builder().build());
        return new UseCaseResult<>(result, context, CheckAccessibilityVerification::new);
    }
}
