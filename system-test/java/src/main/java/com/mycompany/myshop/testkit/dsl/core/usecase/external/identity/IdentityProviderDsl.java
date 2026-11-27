package com.mycompany.myshop.testkit.dsl.core.usecase.external.identity;

import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.identity.usecases.GoToIdentityProvider;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

/**
 * Not closeable, unlike the other external DSLs: {@link com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl} closes the identity
 * provider driver itself, last, because the shops' sessions may still be signed in as a user it created.
 */
public class IdentityProviderDsl {
    protected final IdentityProviderDriver driver;
    protected final UseCaseContext context;

    public IdentityProviderDsl(IdentityProviderDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }

    public GoToIdentityProvider goToIdentityProvider() {
        return new GoToIdentityProvider(driver, context);
    }
}
