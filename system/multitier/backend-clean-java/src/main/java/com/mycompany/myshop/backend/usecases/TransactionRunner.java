package com.mycompany.myshop.backend.usecases;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TransactionRunner {

    // The predicate decides commit or rollback, because a use case that answers with an error has
    // already caught whatever went wrong: there is no exception left for the transaction to see.
    <T> T inTransaction(Supplier<T> work, Predicate<T> commitWhen);
}
