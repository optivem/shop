package com.mycompany.myshop.backend.infrastructure.transaction;

import com.mycompany.myshop.backend.usecases.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work, Predicate<T> commitWhen) {
        return transactionTemplate.execute(status -> {
            var value = work.get();
            if (!commitWhen.test(value)) {
                status.setRollbackOnly();
            }
            return value;
        });
    }
}
