package com.mycompany.myshop.backend.usecases;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);
}
