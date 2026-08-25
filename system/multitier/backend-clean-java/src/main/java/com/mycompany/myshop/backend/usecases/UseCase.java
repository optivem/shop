package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;

public interface UseCase<TRequest, TResponse> {

    Result<TResponse, UseCaseError> execute(TRequest request);
}
