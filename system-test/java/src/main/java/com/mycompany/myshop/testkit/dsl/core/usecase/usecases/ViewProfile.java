package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

public class ViewProfile extends BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification> {
    private String whenAskedToSignIn;

    public ViewProfile(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    /** A visitor asked to sign in signs in as the customer (UI only). */
    public ViewProfile signInWhenAsked() {
        this.whenAskedToSignIn = ViewProfileRequest.SIGN_IN;
        return this;
    }

    @Override
    public UseCaseResult<ViewProfileResponse, ViewProfileVerification> execute() {
        var result = driver.viewProfile(ViewProfileRequest.builder().whenAskedToSignIn(whenAskedToSignIn).build());
        return new UseCaseResult<>(result, context, ViewProfileVerification::new);
    }
}
