package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;

// Transaction demarcation is invisible to the caller too, so it composes at the edge rather than
// being a collaborator the use case has to remember to call.
//
// The subtlety that makes this work: PlaceOrder catches its own pipeline's ValidationException and
// answers with an error, so by the time control reaches here there is no exception left to trigger a
// rollback. Committing on that would leave the order inserted and the coupon unredeemed. Hence the
// rule this decorator enforces instead, which is the stronger one anyway: a use case that answers
// with an error commits nothing.
public class TransactionalUseCase<TRequest, TResponse> implements UseCase<TRequest, TResponse> {

    private final TransactionRunner transactionRunner;
    private final UseCase<TRequest, TResponse> delegate;

    public TransactionalUseCase(TransactionRunner transactionRunner, UseCase<TRequest, TResponse> delegate) {
        this.transactionRunner = transactionRunner;
        this.delegate = delegate;
    }

    @Override
    public Result<TResponse, UseCaseError> execute(TRequest request) {
        return transactionRunner.inTransaction(() -> delegate.execute(request), Result::isOk);
    }
}
