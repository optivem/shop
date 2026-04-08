package com.optivem.shop.systemtest.legacy.mod03.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.playwright.*;
import com.optivem.shop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.shop.systemtest.configuration.Configuration;
import com.optivem.shop.dsl.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;

public class BaseRawTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected Playwright shopUiPlaywright;
    protected Browser shopUiBrowser;
    protected BrowserContext shopUiBrowserContext;
    protected Page shopUiPage;

    protected HttpClient shopApiHttpClient;
    protected HttpClient erpHttpClient;

    protected ObjectMapper httpObjectMapper;

    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpShopBrowser() {
        shopUiPlaywright = Playwright.create();

        var launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true);

        shopUiBrowser = shopUiPlaywright.chromium().launch(launchOptions);

        var contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setStorageStatePath(null);

        shopUiBrowserContext = shopUiBrowser.newContext(contextOptions);
        shopUiPage = shopUiBrowserContext.newPage();
    }

    protected void setUpShopHttpClient() {
        shopApiHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        if (httpObjectMapper == null) {
            httpObjectMapper = createObjectMapper();
        }
    }

    protected void setUpExternalHttpClients() {
        erpHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        httpObjectMapper = createObjectMapper();
    }

    protected String getShopApiBaseUrl() {
        return configuration.getShopApiBaseUrl();
    }

    protected String getShopUiBaseUrl() {
        return configuration.getShopUiBaseUrl();
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
        Closer.close(shopUiPage);
        Closer.close(shopUiBrowserContext);
        Closer.close(shopUiBrowser);
        Closer.close(shopUiPlaywright);
        Closer.close(erpHttpClient);
        Closer.close(shopApiHttpClient);
    }
}
