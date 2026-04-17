import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import type { ShopDriver } from '../../../../src/testkit/driver/port/shop/shop-driver.js';
import { ShopApiDriver } from '../../../../src/testkit/driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from '../../../../src/testkit/driver/adapter/shop/ui/shop-ui-driver.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

export const apiTest = base.extend<{ shopDriver: ShopDriver }>({
    shopDriver: async ({}, use) => {
        const driver = new ShopApiDriver(config.shop.backendApiUrl);
        await use(driver);
        await driver.close();
    },
});

export const uiTest = base.extend<{ shopDriver: ShopDriver; _shopBrowser: Browser }>({
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
});

export { expect } from '@playwright/test';
