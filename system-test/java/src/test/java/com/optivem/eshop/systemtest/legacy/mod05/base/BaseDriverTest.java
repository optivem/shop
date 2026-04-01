package com.optivem.eshop.systemtest.legacy.mod05.base;

import com.optivem.eshop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.eshop.dsl.core.usecase.Configuration;
import com.optivem.eshop.dsl.driver.adapter.external.erp.ErpRealDriver;
import com.optivem.eshop.dsl.driver.adapter.shop.api.ShopApiDriver;
import com.optivem.eshop.dsl.driver.port.shop.ShopDriver;
import com.optivem.eshop.dsl.driver.adapter.shop.ui.ShopUiDriver;
import com.optivem.eshop.dsl.driver.adapter.external.tax.TaxRealDriver;
import com.optivem.eshop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import com.optivem.eshop.dsl.common.Closer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseDriverTest extends BaseConfigurableTest {
    protected Configuration configuration;

    protected ShopDriver shopDriver;
    protected ErpRealDriver erpDriver;
    protected TaxRealDriver taxDriver;


    @BeforeEach
    protected void setUpConfiguration() {
        configuration = loadConfiguration();
    }

    protected void setUpShopUiDriver() {
        shopDriver = new ShopUiDriver(configuration.getShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser());
    }

    protected void setUpShopApiDriver() {
        shopDriver = new ShopApiDriver(configuration.getShopApiBaseUrl());
    }

    protected void setUpExternalDrivers() {
        erpDriver = new ErpRealDriver(configuration.getErpBaseUrl());
        taxDriver = new TaxRealDriver(configuration.getTaxBaseUrl());
    }

    @AfterEach
    void tearDown() {
        Closer.close(shopDriver);
        Closer.close(erpDriver);
        Closer.close(taxDriver);
    }
}

