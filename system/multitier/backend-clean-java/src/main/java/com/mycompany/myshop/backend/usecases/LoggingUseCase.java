package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A cross-cutting concern belongs in a decorator when it is invisible to the caller: it does not
// change the answer, so nothing about it needs to appear in a use case's signature. Logging, timing,
// metrics and correlation ids are all that shape. A business refusal is NOT -- it is the answer, so
// it travels as a returned value instead and this class only observes it going past.
public class LoggingUseCase<TRequest, TResponse> implements UseCase<TRequest, TResponse> {

    private static final Logger log = LoggerFactory.getLogger(LoggingUseCase.class);

    private final String name;
    private final UseCase<TRequest, TResponse> delegate;

    public LoggingUseCase(String name, UseCase<TRequest, TResponse> delegate) {
        this.name = name;
        this.delegate = delegate;
    }

    @Override
    public Result<TResponse, UseCaseError> execute(TRequest request) {
        var startedAt = System.nanoTime();
        try {
            var result = delegate.execute(request);
            // A rejection is an ordinary answer, so it is logged at info beside a success. Only a
            // thrown exception is a defect, and that is the one thing logged at error.
            log.info("{} {} in {}ms", name, result.isOk() ? "ok" : "rejected", millisSince(startedAt));
            return result;
        } catch (RuntimeException e) {
            // Log and rethrow, never translate: converting a defect into a UseCaseError would let a
            // bug report itself to the caller as a business rule, and turn a 500 into a 422.
            log.error("{} failed after {}ms", name, millisSince(startedAt), e);
            throw e;
        }
    }

    private static long millisSince(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
