package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base;

import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.BaseUseCase;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;

public abstract class BaseMyShopUseCase<R, V extends ResponseVerification<R>>
        extends BaseUseCase<MyShopDriver, R, V> {

    protected final UseCaseContext context;

    protected BaseMyShopUseCase(MyShopDriver driver, UseCaseContext context) {
        super(driver);
        this.context = context;
    }
}
