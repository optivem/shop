package com.mycompany.myshop.backend.usecases;

public interface UseCase<TRequest, TResponse> {

    Result<TResponse, UseCaseError> execute(TRequest request);
}
