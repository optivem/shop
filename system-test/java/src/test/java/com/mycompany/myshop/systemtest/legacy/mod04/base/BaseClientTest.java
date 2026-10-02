package com.mycompany.myshop.systemtest.legacy.mod04.base;

import com.mycompany.myshop.systemtest.configuration.BaseConfigurableTest;
import com.mycompany.myshop.systemtest.configuration.Configuration;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.client.ErpRealClient;
import com.mycompany.myshop.testkit.driver.adapter.external.tax.client.TaxRealClient;
import com.mycompany.myshop.testkit.driver.adapter.api.client.MyShopApiClient;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.MyShopUiClient;
import com.mycompany.myshop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import com.mycompany.myshop.testkit.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseClientTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected MyShopUiClient myShopUiClient;
    protected MyShopApiClient myShopApiClient;
    protected ErpRealClient erpClient;
    protected TaxRealClient taxClient;

    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpMyShopUiClient() {
        myShopUiClient = new MyShopUiClient(configuration.getMyShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser(), configuration.getKeycloakBaseUrl());
    }

    protected void setUpMyShopApiClient() {
        myShopApiClient = new MyShopApiClient(configuration.getMyShopApiBaseUrl(), configuration.getKeycloakBaseUrl());
    }

    protected void setUpExternalClients() {
        erpClient = new ErpRealClient(configuration.getErpBaseUrl());
        taxClient = new TaxRealClient(configuration.getTaxBaseUrl());
    }

    @AfterEach
    void tearDown() {
        Closer.close(myShopUiClient);
        Closer.close(myShopApiClient);
        Closer.close(erpClient);
        Closer.close(taxClient);
    }

}
