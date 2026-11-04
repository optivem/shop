package com.mycompany.myshop.backend.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.core.dtos.CurrentUser;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderRequest;
import com.mycompany.myshop.backend.core.dtos.PlaceOrderResponse;
import com.mycompany.myshop.backend.core.entities.IdempotencyKey;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyKeyStoreTest {

    private static final Instant NOW = Instant.parse("2026-11-03T12:00:00Z");
    private static final CurrentUser CUSTOMER = new CurrentUser("customer-sub", "customer1", false);

    @Mock
    private IdempotencyKeyRepository repository;

    private IdempotencyKeyStore store;

    @BeforeEach
    void setUp() {
        store = new IdempotencyKeyStore(repository, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void blankHeaderIsNoKey() {
        assertThat(IdempotencyKeyStore.normalizeKey(null)).isEmpty();
        assertThat(IdempotencyKeyStore.normalizeKey("  ")).isEmpty();
        assertThat(IdempotencyKeyStore.normalizeKey("key-1")).contains("key-1");
    }

    @Test
    void keyLongerThan255CharactersIsRejected() {
        assertThat(IdempotencyKeyStore.normalizeKey("k".repeat(255))).isPresent();

        var thrown = catchThrowableOfType(ValidationException.class, () -> IdempotencyKeyStore.normalizeKey("k".repeat(256)));

        assertThat(thrown.getFieldName()).isEqualTo("Idempotency-Key");
    }

    @Test
    void claimIsScopedToTheCallerAndAnonymousIsTheEmptyOwner() {
        assertThat(IdempotencyKeyStore.claim("key-1", CUSTOMER, request("BOOK-123", 1)).owner()).isEqualTo("customer-sub");
        assertThat(IdempotencyKeyStore.claim("key-1", new CurrentUser(null, null, false), request("BOOK-123", 1)).owner()).isEmpty();
    }

    @Test
    void fingerprintIsTheSha256OfTheFieldsJoinedByNewlines() {
        assertThat(IdempotencyKeyStore.fingerprint(request("BOOK-123", 2)))
                .isEqualTo(sha256("BOOK-123\n2\nUS\n"))
                .hasSize(64);
        assertThat(IdempotencyKeyStore.fingerprint(request("BOOK-123", 3)))
                .isNotEqualTo(IdempotencyKeyStore.fingerprint(request("BOOK-123", 2)));
    }

    @Test
    void unusedKeyHasNoReplay() {
        var claim = IdempotencyKeyStore.claim("key-1", CUSTOMER, request("BOOK-123", 1));
        when(repository.findByOwnerAndKey("customer-sub", "key-1")).thenReturn(Optional.empty());

        assertThat(store.findReplay(claim, PlaceOrderResponse.class)).isEmpty();
    }

    @Test
    void sameRequestReplaysTheStoredResponse() {
        var claim = IdempotencyKeyStore.claim("key-1", CUSTOMER, request("BOOK-123", 1));
        when(repository.findByOwnerAndKey("customer-sub", "key-1"))
                .thenReturn(Optional.of(row(claim.requestHash(), NOW.minus(Duration.ofHours(23)))));

        var replay = store.findReplay(claim, PlaceOrderResponse.class).orElseThrow();

        assertThat(replay.status()).isEqualTo(201);
        assertThat(replay.body().getOrderNumber()).isEqualTo("ORD-1");
    }

    @Test
    void differentRequestWithTheSameKeyIsRejected() {
        var claim = IdempotencyKeyStore.claim("key-1", CUSTOMER, request("BOOK-123", 2));
        when(repository.findByOwnerAndKey("customer-sub", "key-1"))
                .thenReturn(Optional.of(row(IdempotencyKeyStore.fingerprint(request("BOOK-123", 1)), NOW)));

        var thrown = catchThrowableOfType(ValidationException.class, () -> store.findReplay(claim, PlaceOrderResponse.class));

        assertThat(thrown.getFieldName()).isNull();
        assertThat(thrown).hasMessage("Idempotency-Key has already been used with a different request");
    }

    @Test
    void expiredKeyCountsAsUnused() {
        var claim = IdempotencyKeyStore.claim("key-1", CUSTOMER, request("BOOK-123", 2));
        when(repository.findByOwnerAndKey("customer-sub", "key-1"))
                .thenReturn(Optional.of(row("another-hash", NOW.minus(Duration.ofHours(24)))));

        assertThat(store.findReplay(claim, PlaceOrderResponse.class)).isEmpty();
    }

    @Test
    void recordReplacesOnlyAnExpiredRowAndStoresTheResponseJson() {
        var claim = new IdempotencyKeyStore.Claim("customer-sub", "key-1", "hash");
        var response = new PlaceOrderResponse();
        response.setOrderNumber("ORD-1");

        store.record(claim, 201, response);

        verify(repository).deleteExpired("customer-sub", "key-1", NOW.minus(Duration.ofHours(24)));
        var saved = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getResponseBody()).isEqualTo("{\"orderNumber\":\"ORD-1\"}");
        assertThat(saved.getValue().getResponseStatus()).isEqualTo(201);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    private static IdempotencyKey row(String requestHash, Instant createdAt) {
        return new IdempotencyKey("customer-sub", "key-1", requestHash, 201, "{\"orderNumber\":\"ORD-1\"}", createdAt);
    }

    private static PlaceOrderRequest request(String sku, int quantity) {
        var request = new PlaceOrderRequest();
        request.setSku(sku);
        request.setQuantity(quantity);
        request.setCountry("US");
        return request;
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
