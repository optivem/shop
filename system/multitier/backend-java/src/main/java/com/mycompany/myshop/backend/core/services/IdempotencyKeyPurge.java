package com.mycompany.myshop.backend.core.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Purges expired {@code idempotency_keys} rows on a fixed delay ({@code idempotency.purge-interval},
 * default one hour). Every instance runs it; the DELETE is idempotent, so overlapping runs are harmless.
 */
@Component
public class IdempotencyKeyPurge {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyPurge.class);

    private final IdempotencyKeyStore store;

    public IdempotencyKeyPurge(IdempotencyKeyStore store) {
        this.store = store;
    }

    @Scheduled(initialDelayString = "${idempotency.purge-interval:PT1H}", fixedDelayString = "${idempotency.purge-interval:PT1H}")
    public void purge() {
        var deleted = store.purgeExpired();
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
