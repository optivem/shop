package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;

// The one place in the application where a thrown refusal becomes an answered error.
//
// It is a decorator rather than a try/catch inside each use case, and that is the whole design: a
// use case cannot forget to handle something it never handles. Every use case is wrapped here by
// construction in UseCaseConfig, so one written tomorrow answers its refusals with a 422 without a
// line of code being written for them -- which is the guarantee a per-use-case catch could only
// promise and a code review could only police.
//
// Only ValidationException is caught. A GatewayException is not a refusal but an interruption, and
// any other unchecked exception is a defect; both travel past untouched to the handlers that answer
// them with 502 and 500. Catching wider here is exactly how a bug starts reporting itself to the
// caller as a business rule.
public class RefusalTranslatingUseCase<TRequest, TResponse> implements UseCase<TRequest, TResponse> {

    private final UseCase<TRequest, TResponse> delegate;

    public RefusalTranslatingUseCase(UseCase<TRequest, TResponse> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Result<TResponse, UseCaseError> execute(TRequest request) {
        try {
            return delegate.execute(request);
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }
    }
}
