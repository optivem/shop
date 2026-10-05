package com.mycompany.myshop.systemtest.latest.acceptance.base;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class BaseIdentityAcceptanceTest extends BaseAcceptanceTest {
    @BeforeEach
    void requireKeycloak() {
        var keycloakBaseUrl = loadConfiguration().getKeycloakBaseUrl();
        assumeTrue(keycloakBaseUrl != null && !keycloakBaseUrl.isBlank(),
                "Identity scenarios require KEYCLOAK_URL to be set");
    }
}
