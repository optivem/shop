package com.mycompany.myshop.systemtest.legacy.mod03.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.playwright.*;
import com.mycompany.myshop.systemtest.configuration.BaseConfigurableTest;
import com.mycompany.myshop.systemtest.configuration.Configuration;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.KeycloakUiLogin;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.KeycloakTokenProvider;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public abstract class BaseRawTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected Playwright myShopUiPlaywright;
    protected Browser myShopUiBrowser;
    protected BrowserContext myShopUiBrowserContext;
    protected Page myShopUiPage;

    protected HttpClient myShopApiHttpClient;
    protected HttpClient erpHttpClient;

    protected ObjectMapper httpObjectMapper;

    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpMyShopBrowser() {
        myShopUiPlaywright = Playwright.create();

        var launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true);

        myShopUiBrowser = myShopUiPlaywright.chromium().launch(launchOptions);

        var contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setStorageStatePath(null);

        myShopUiBrowserContext = myShopUiBrowser.newContext(contextOptions);
        myShopUiPage = myShopUiBrowserContext.newPage();
    }

    protected void setUpMyShopHttpClient() {
        myShopApiHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        if (httpObjectMapper == null) {
            httpObjectMapper = createObjectMapper();
        }
    }

    protected void setUpExternalHttpClients() {
        erpHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        httpObjectMapper = createObjectMapper();
    }

    protected String getMyShopApiBaseUrl() {
        return configuration.getMyShopApiBaseUrl();
    }

    protected HttpRequest.Builder withCustomerAuth(HttpRequest.Builder builder) {
        var keycloakBaseUrl = configuration.getKeycloakBaseUrl();
        if (keycloakBaseUrl == null || keycloakBaseUrl.isBlank()) {
            return builder;
        }
        var token = KeycloakTokenProvider.forBaseUrl(keycloakBaseUrl).getToken(TestUser.CUSTOMER);
        return builder.header("Authorization", "Bearer " + token);
    }

    protected void loginToMyShopUiIfRequired() {
        var login = KeycloakUiLogin.forBaseUrl(configuration.getKeycloakBaseUrl());
        if (login != null) {
            login.ensureLoggedIn(myShopUiPage, "a[href='/new-order']");
        }
    }

    protected String getMyShopUiBaseUrl() {
        return configuration.getMyShopUiBaseUrl();
    }

    protected String getErpBaseUrl() {
        return configuration.getErpBaseUrl();
    }

    private ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @AfterEach
    void tearDown() {
        Closer.close(myShopUiPage);
        Closer.close(myShopUiBrowserContext);
        Closer.close(myShopUiBrowser);
        Closer.close(myShopUiPlaywright);
        Closer.close(erpHttpClient);
        Closer.close(myShopApiHttpClient);
    }
}
