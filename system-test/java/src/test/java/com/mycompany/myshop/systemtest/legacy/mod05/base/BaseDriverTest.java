package com.mycompany.myshop.systemtest.legacy.mod05.base;

import com.mycompany.myshop.systemtest.configuration.BaseConfigurableTest;
import com.mycompany.myshop.systemtest.configuration.Configuration;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.ErpRealDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.tax.TaxRealDriver;
import com.mycompany.myshop.testkit.driver.adapter.api.MyShopApiDriver;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.adapter.ui.MyShopUiDriver;
import com.mycompany.myshop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import com.mycompany.myshop.testkit.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseDriverTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected MyShopDriver myShopDriver;
    protected ErpRealDriver erpDriver;
    protected TaxRealDriver taxDriver;

    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpMyShopUiDriver() {
        myShopDriver = new MyShopUiDriver(configuration.getMyShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser(), configuration.getKeycloakBaseUrl());
    }

    protected void setUpMyShopApiDriver() {
        myShopDriver = new MyShopApiDriver(configuration.getMyShopApiBaseUrl(), configuration.getKeycloakBaseUrl());
    }

    protected void setUpExternalDrivers() {
        erpDriver = new ErpRealDriver(configuration.getErpBaseUrl());
        taxDriver = new TaxRealDriver(configuration.getTaxBaseUrl());
    }

    @AfterEach
    void tearDown() {
        Closer.close(myShopDriver);
        Closer.close(erpDriver);
        Closer.close(taxDriver);
    }
}
