package com.optivem.eshop.systemtest.legacy.mod02.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.playwright.*;
import com.optivem.eshop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.eshop.dsl.core.usecase.Configuration;
import com.optivem.eshop.dsl.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpClient;
import java.util.UUID;

public class BaseRawTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected Playwright shopUiPlaywright;
    protected Browser shopUiBrowser;
    protected BrowserContext shopUiBrowserContext;
    protected Page shopUiPage;

    protected HttpClient shopApiHttpClient;
    protected HttpClient erpHttpClient;
    protected HttpClient taxHttpClient;

    protected ObjectMapper httpObjectMapper;

    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpShopBrowser() {
        shopUiPlaywright = Playwright.create();

        var launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setSlowMo(100);

        shopUiBrowser = shopUiPlaywright.chromium().launch(launchOptions);

        var contextOptions = new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setStorageStatePath(null);

        shopUiBrowserContext = shopUiBrowser.newContext(contextOptions);
        shopUiPage = shopUiBrowserContext.newPage();
    }

    protected void setUpShopHttpClient() {
        shopApiHttpClient = HttpClient.newHttpClient();
        if (httpObjectMapper == null) {
            httpObjectMapper = createObjectMapper();
        }
    }

    protected void setUpExternalHttpClients() {
        erpHttpClient = HttpClient.newHttpClient();
        taxHttpClient = HttpClient.newHttpClient();
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

    protected String getTaxBaseUrl() {
        return configuration.getTaxBaseUrl();
    }

    private ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    protected String createUniqueSku(String baseSku) {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSku + "-" + suffix;
    }

    @AfterEach
    void tearDown() {
        Closer.close(shopUiPage);
        Closer.close(shopUiBrowserContext);
        Closer.close(shopUiBrowser);
        Closer.close(shopUiPlaywright);
        Closer.close(erpHttpClient);
        Closer.close(taxHttpClient);
        Closer.close(shopApiHttpClient);
    }
}


