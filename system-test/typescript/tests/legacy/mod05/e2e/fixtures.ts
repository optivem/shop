import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import type { ShopDriver } from '../../../../src/testkit/driver/port/shop/shop-driver.js';
import type { ErpDriver } from '../../../../src/testkit/driver/port/external/erp/erp-driver.js';
import type { TaxDriver } from '../../../../src/testkit/driver/port/external/tax/tax-driver.js';
import { ShopApiDriver } from '../../../../src/testkit/driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from '../../../../src/testkit/driver/adapter/shop/ui/shop-ui-driver.js';
import { ErpStubDriver } from '../../../../src/testkit/driver/adapter/external/erp/erp-stub-driver.js';
import { TaxStubDriver } from '../../../../src/testkit/driver/adapter/external/tax/tax-stub-driver.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

// Driver fixtures for API tests
export const apiTest = base.extend<{ shopDriver: ShopDriver; erpDriver: ErpDriver; taxDriver: TaxDriver }>({
    shopDriver: async ({}, use) => {
        const driver = new ShopApiDriver(config.shop.backendApiUrl);
        await use(driver);
        await driver.close();
    },
    erpDriver: async ({}, use) => {
        const driver = new ErpStubDriver(config.externalSystems.erp.url);
        await use(driver);
        await driver.close();
    },
    taxDriver: async ({}, use) => {
        const driver = new TaxStubDriver(config.externalSystems.tax.url);
        await use(driver);
        await driver.close();
    },
});

// Driver fixtures for UI tests
export const uiTest = base.extend<{ shopDriver: ShopDriver; erpDriver: ErpDriver; taxDriver: TaxDriver; _shopBrowser: Browser }>({
    _shopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    shopDriver: async ({ _shopBrowser }, use) => {
        const driver = new ShopUiDriver(config.shop.frontendUrl, _shopBrowser);
        await use(driver);
        await driver.close();
    },
    erpDriver: async ({}, use) => {
        const driver = new ErpStubDriver(config.externalSystems.erp.url);
        await use(driver);
        await driver.close();
    },
    taxDriver: async ({}, use) => {
        const driver = new TaxStubDriver(config.externalSystems.tax.url);
        await use(driver);
        await driver.close();
    },
});

export { expect } from '@playwright/test';
