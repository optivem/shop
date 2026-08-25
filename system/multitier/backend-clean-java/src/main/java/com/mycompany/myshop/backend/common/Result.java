package com.mycompany.myshop.backend.common;

import java.util.Optional;

public sealed interface Result<T, E> {

    boolean isOk();

    T value();

    E error();

    record Ok<T, E>(T value) implements Result<T, E> {

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public E error() {
            throw new IllegalStateException("A successful Result has no error");
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public T value() {
            throw new IllegalStateException("A failed Result has no value");
        }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    // A Result<Void, E> has nothing to carry, so success says so by taking no argument.
    static <E> Result<Void, E> ok() {
        return new Ok<>(null);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    // For a caller that has its own answer for the failed case and does not need the error.
    default Optional<T> toOptional() {
        return isOk() ? Optional.of(value()) : Optional.empty();
    }
}
