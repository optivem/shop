package com.optivem.eshop.dsl.common;

public class Result<T, E> {
    private final boolean success;
    private final T value;
    private final E error;

    private Result(boolean success, T value, E error) {
        this.success = success;
        this.value = value;
        this.error = error;
    }

    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(true, value, null);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(false, null, error);
    }

    public static <E> Result<Void, E> success() {
        return new Result<>(true, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getValue() {
        if (!success) {
            throw new IllegalStateException("Cannot get value from a failed result");
        }
        return value;
    }

    public E getError() {
        if (success) {
            throw new IllegalStateException("Cannot get error from a successful result");
        }
        return error;
    }

    public <T2> Result<T2, E> map(java.util.function.Function<T, T2> mapper) {
        if (success) {
            return Result.success(mapper.apply(value));
        }
        return Result.failure(error);
    }

    public <E2> Result<T, E2> mapError(java.util.function.Function<E, E2> mapper) {
        if (success) {
            return Result.success(value);
        }
        return Result.failure(mapper.apply(error));
    }

    public Result<Void, E> mapVoid() {
        if (success) {
            return Result.success();
        }
        return Result.failure(error);
    }
}


