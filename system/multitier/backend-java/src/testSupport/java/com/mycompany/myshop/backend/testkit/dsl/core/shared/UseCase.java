package com.mycompany.myshop.backend.support.core.shared;

/** A use case against the system under test: executing it produces an outcome to be asserted. */
public interface UseCase<R> {
    R execute();
}
