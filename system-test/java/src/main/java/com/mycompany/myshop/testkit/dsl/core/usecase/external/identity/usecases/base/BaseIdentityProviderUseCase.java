package com.mycompany.myshop.testkit.dsl.core.usecase.external.identity.usecases.base;

import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;
import com.mycompany.myshop.testkit.dsl.core.shared.BaseUseCase;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

public abstract class BaseIdentityProviderUseCase<R, V> extends BaseUseCase<IdentityProviderDriver, R, V> {
    protected BaseIdentityProviderUseCase(IdentityProviderDriver driver, UseCaseContext context) {
        super(driver, context);
    }
}
