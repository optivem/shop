package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

public class ViewOrder extends BaseMyShopUseCase<ViewOrderResponse, ViewOrderVerification> {
    private String orderNumberResultAlias;
    private String whenAskedToSignIn;

    public ViewOrder(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ViewOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    /** A visitor asked to sign in signs in as the customer (UI only). */
    public ViewOrder signInWhenAsked() {
        this.whenAskedToSignIn = ViewOrderRequest.SIGN_IN;
        return this;
    }

    @Override
    public UseCaseResult<ViewOrderResponse, ViewOrderVerification> execute() {
        var orderNumber = context.getResultValue(orderNumberResultAlias);

        var request = ViewOrderRequest.builder().orderNumber(orderNumber).whenAskedToSignIn(whenAskedToSignIn).build();
        var result = driver.viewOrder(request);

        return new UseCaseResult<>(result, context, ViewOrderVerification::new);
    }
}



