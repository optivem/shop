package com.mycompany.myshop.backend.usecases;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TransactionRunner {

    // A thrown refusal rolls back the ordinary way, by propagating. The predicate is for the other
    // shape: a use case that answers with a returned error, where there is no exception for the
    // transaction to see.
    <T> T inTransaction(Supplier<T> work, Predicate<T> commitWhen);
}
