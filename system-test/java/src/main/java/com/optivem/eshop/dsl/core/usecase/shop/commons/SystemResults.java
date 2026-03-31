package com.optivem.eshop.dsl.core.usecase.shop.commons;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

public final class SystemResults {
    private SystemResults() {
        // Utility class
    }

    public static <T> Result<T, ErrorResponse> success(T value) {
        return Result.success(value);
    }

    public static Result<Void, ErrorResponse> success() {
        return Result.success();
    }

    public static <T> Result<T, ErrorResponse> failure(String message) {
        return Result.failure(ErrorResponse.of(message));
    }

    public static <T> Result<T, ErrorResponse> failure(ErrorResponse error) {
        return Result.failure(error);
    }
}

