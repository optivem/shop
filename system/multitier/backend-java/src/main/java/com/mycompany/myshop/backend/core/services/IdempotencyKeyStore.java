package com.mycompany.myshop.backend.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.entities.IdempotencyKey;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.IdempotencyKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

/**
 * Stores and replays the responses of requests sent with an {@code Idempotency-Key} header.
 *
 * <p>A key belongs to the caller who used it (the token subject; the empty string for an anonymous
 * caller) and lives {@link #TTL} from first use. Using it again with the same request returns the
 * stored response; using it with a different request is a 422. Only successful responses are
 * stored -- a failed request rolls its transaction back, row included -- so a corrected retry can
 * reuse the key.
 */
@Service
public class IdempotencyKeyStore {

    public static final String HEADER = "Idempotency-Key";
    public static final Duration TTL = Duration.ofHours(24);
    public static final int MAX_KEY_LENGTH = 255;

    static final String MSG_KEY_TOO_LONG = HEADER + " must be at most " + MAX_KEY_LENGTH + " characters";
    static final String MSG_KEY_REUSED = HEADER + " has already been used with a different request";

    private static final String FINGERPRINT_SEPARATOR = "\n";

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public IdempotencyKeyStore(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, Clock.systemUTC());
    }

    IdempotencyKeyStore(IdempotencyKeyRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** A blank header is no header. A key longer than the column is a validation error. */
    public static Optional<String> normalizeKey(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return Optional.empty();
        }
        if (headerValue.length() > MAX_KEY_LENGTH) {
            throw new ValidationException(HEADER, MSG_KEY_TOO_LONG);
        }
        return Optional.of(headerValue);
    }

    /** The key, scoped to its caller, plus the fingerprint of the request it is being used with. */
    public static Claim claim(String key, CurrentUser user, PlaceOrderRequest request) {
        var owner = user.subject() == null ? "" : user.subject();
        return new Claim(owner, key, fingerprint(request));
    }

    /** SHA-256 (hex) over sku, quantity, country and couponCode as received, null as empty, joined by a newline. */
    static String fingerprint(PlaceOrderRequest request) {
        var canonical = String.join(FINGERPRINT_SEPARATOR,
                Objects.toString(request.getSku(), ""),
                Objects.toString(request.getQuantity(), ""),
                Objects.toString(request.getCountry(), ""),
                Objects.toString(request.getCouponCode(), ""));
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * The stored response for this claim, if its key was already used inside the TTL with the same
     * request. Throws a 422 if it was used with a different one. An expired row counts as unused.
     */
    @Transactional(readOnly = true)
    public <T> Optional<StoredResponse<T>> findReplay(Claim claim, Class<T> bodyType) {
        var stored = repository.findByOwnerAndKey(claim.owner(), claim.key())
                .filter(row -> !isExpired(row));
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        var row = stored.get();
        if (!row.getRequestHash().equals(claim.requestHash())) {
            throw new ValidationException(MSG_KEY_REUSED);
        }
        return Optional.of(new StoredResponse<>(row.getResponseStatus(), readBody(row.getResponseBody(), bodyType)));
    }

    /**
     * Records the response under the claim, inside the caller's transaction. An expired row for the
     * same key is replaced; a live one makes the insert fail on the unique constraint, which rolls the
     * caller's transaction back.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Claim claim, int status, Object body) {
        var now = clock.instant();
        repository.deleteExpired(claim.owner(), claim.key(), now.minus(TTL));
        repository.saveAndFlush(new IdempotencyKey(claim.owner(), claim.key(), claim.requestHash(), status, writeBody(body), now));
    }

    private boolean isExpired(IdempotencyKey row) {
        return !row.getCreatedAt().isAfter(clock.instant().minus(TTL));
    }

    private String writeBody(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the response to store it", e);
        }
    }

    private <T> T readBody(String body, Class<T> bodyType) {
        try {
            return objectMapper.readValue(body, bodyType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read the stored response", e);
        }
    }

    public record Claim(String owner, String key, String requestHash) {
    }

    public record StoredResponse<T>(int status, T body) {
    }
}
