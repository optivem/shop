package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.SignUpRequest;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

/** A visitor signs up (UC-013) with this account and shipping country (UI only). */
public class SignUp extends BaseMyShopUseCase<Void, VoidVerification> {
    private final SignUpRequest.SignUpRequestBuilder request = SignUpRequest.builder();

    public SignUp(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public SignUp account(String username, String password) {
        request.username(username).password(password);
        return this;
    }

    public SignUp email(String email) {
        request.email(email);
        return this;
    }

    public SignUp name(String firstName, String lastName) {
        request.firstName(firstName).lastName(lastName);
        return this;
    }

    public SignUp country(String country) {
        request.country(country);
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.signUp(request.build()).mapVoid();
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
