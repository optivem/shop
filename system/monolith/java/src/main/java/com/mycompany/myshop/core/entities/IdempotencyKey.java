package com.mycompany.myshop.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * One used {@code Idempotency-Key}: who used it, the fingerprint of the request it was used with, and
 * the response that request got. Written in the same transaction as the order it answers for.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "idempotency_key", nullable = false)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKey(String owner, String key, String requestHash, int responseStatus, String responseBody, Instant createdAt) {
        this.owner = owner;
        this.key = key;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    /**
     * Identity is the database id, so an entity stays equal to itself before and after it is persisted
     * and across Hibernate proxies; two unsaved entities are never equal. The hash code is constant per
     * class so it does not change when the id is assigned on save.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdempotencyKey that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return IdempotencyKey.class.hashCode();
    }
}
