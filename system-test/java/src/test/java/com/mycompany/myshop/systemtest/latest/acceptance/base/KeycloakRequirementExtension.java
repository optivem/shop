package com.mycompany.myshop.systemtest.latest.acceptance.base;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class KeycloakRequirementExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        var requiresKeycloak = context.getTestMethod()
                .map(method -> method.isAnnotationPresent(RequiresKeycloak.class))
                .orElse(false);
        if (!requiresKeycloak) {
            return;
        }

        var testInstance = (BaseAcceptanceTest) context.getRequiredTestInstance();
        assumeTrue(testInstance.isKeycloakConfigured(), "Identity scenarios require KEYCLOAK_URL to be set");
    }
}
