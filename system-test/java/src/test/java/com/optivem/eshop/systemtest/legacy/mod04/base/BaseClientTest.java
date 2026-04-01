package com.optivem.eshop.systemtest.legacy.mod04.base;

import com.optivem.eshop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.eshop.dsl.core.usecase.Configuration;
import com.optivem.eshop.dsl.driver.adapter.external.erp.client.ErpRealClient;
import com.optivem.eshop.dsl.driver.adapter.shop.api.client.ShopApiClient;
import com.optivem.eshop.dsl.driver.adapter.shop.ui.client.ShopUiClient;
import com.optivem.eshop.dsl.driver.adapter.external.tax.client.TaxRealClient;
import com.optivem.eshop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import com.optivem.eshop.dsl.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

public class BaseClientTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected ShopUiClient shopUiClient;
    protected ShopApiClient shopApiClient;
    protected ErpRealClient erpClient;
    protected TaxRealClient taxClient;


    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpShopUiClient() {
        shopUiClient = new ShopUiClient(configuration.getShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser());
    }

    protected void setUpShopApiClient() {
        shopApiClient = new ShopApiClient(configuration.getShopApiBaseUrl());
    }

    protected void setUpExternalClients() {
        erpClient = new ErpRealClient(configuration.getErpBaseUrl());
        taxClient = new TaxRealClient(configuration.getTaxBaseUrl());
    }

    @AfterEach
    void tearDown() {
        Closer.close(shopUiClient);
        Closer.close(shopApiClient);
        Closer.close(erpClient);
        Closer.close(taxClient);
    }

    protected String createUniqueSku(String baseSku) {
        var suffix = UUID.randomUUID().toString().substring(0, 8);
        return baseSku + "-" + suffix;
    }
}


