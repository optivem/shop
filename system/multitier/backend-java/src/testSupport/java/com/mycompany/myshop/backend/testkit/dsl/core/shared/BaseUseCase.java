package com.mycompany.myshop.backend.support.core.shared;

public abstract class BaseUseCase<D, R, V extends ResponseVerification<R>>
        implements UseCase<UseCaseResult<R, V>> {

    protected final D driver;

    protected BaseUseCase(D driver) {
        this.driver = driver;
    }
}
