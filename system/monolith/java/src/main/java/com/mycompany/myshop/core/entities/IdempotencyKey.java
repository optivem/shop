package com.mycompany.myshop.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One used {@code Idempotency-Key}: who used it, the fingerprint of the request it was used with, and
 * the response that request got. Written in the same transaction as the order it answers for.
 */
@Entity
@Table(name = "idempotency_keys")
@Data
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
}
