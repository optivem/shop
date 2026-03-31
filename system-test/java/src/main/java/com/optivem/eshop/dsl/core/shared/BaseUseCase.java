package com.optivem.eshop.dsl.core.shared;

public abstract class BaseUseCase<TDriver, TSuccessResponse, TSuccessVerification> implements UseCase<UseCaseResult<TSuccessResponse, TSuccessVerification>> {
    protected final TDriver driver;
    protected final UseCaseContext context;

    protected BaseUseCase(TDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }
}



