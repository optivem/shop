package com.mycompany.myshop.systemtest.latest.acceptance.base;

import com.mycompany.myshop.systemtest.configuration.BaseConfigurableTest;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.driver.adapter.api.client.MyShopApiClient;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class BaseApiClientTest extends BaseConfigurableTest {
    protected MyShopApiClient myShopApiClient;

    @BeforeEach
    void setUpApiClient() {
        var configuration = loadConfiguration();
        var keycloakBaseUrl = configuration.getKeycloakBaseUrl();
        assumeTrue(keycloakBaseUrl != null && !keycloakBaseUrl.isBlank(),
                "Authorization tests require KEYCLOAK_URL to be set");
        myShopApiClient = new MyShopApiClient(configuration.getMyShopApiBaseUrl(), keycloakBaseUrl);
    }

    @AfterEach
    void tearDownApiClient() {
        Closer.close(myShopApiClient);
    }

    @Override
    protected ExternalSystemMode getFixedExternalSystemMode() {
        return ExternalSystemMode.STUB;
    }
}
