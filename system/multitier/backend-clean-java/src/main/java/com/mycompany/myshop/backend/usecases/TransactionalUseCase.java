package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;

// Transaction demarcation is invisible to the caller too, so it composes at the edge rather than
// being a collaborator the use case has to remember to call.
//
// It sits INSIDE RefusalTranslatingUseCase, which is what makes a thrown refusal roll back for free:
// the exception is still an exception when it crosses this boundary, and only becomes a returned
// error one layer out. Translate first and the rollback would have nothing to see.
//
// The commit predicate covers the other way a use case can answer badly -- returning an error rather
// than throwing, as ViewOrderDetails and CancelOrder do for a missing order. None of them write
// before doing so today, but the rule costs nothing and is the stronger one: a use case that answers
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
