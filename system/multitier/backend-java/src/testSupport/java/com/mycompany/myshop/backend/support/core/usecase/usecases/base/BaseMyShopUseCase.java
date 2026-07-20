package com.mycompany.myshop.backend.support.core.usecase.usecases.base;

import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.core.shared.BaseUseCase;
import com.mycompany.myshop.backend.support.core.shared.ResponseVerification;

public abstract class BaseMyShopUseCase<R, V extends ResponseVerification<R>>
        extends BaseUseCase<BackendDriver, R, V> {

    protected BaseMyShopUseCase(BackendDriver driver) {
        super(driver);
    }
}
