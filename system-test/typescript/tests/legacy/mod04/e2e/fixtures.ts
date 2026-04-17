import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext, Page } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { ShopApiClient } from '../../../../src/testkit/driver/adapter/shop/api/client/ShopApiClient.js';
import { ErpRealClient } from '../../../../src/testkit/driver/adapter/external/erp/client/ErpRealClient.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'real';

const config = loadConfiguration();

// Client fixtures for API tests
export const apiTest = base.extend<{ shopApiClient: ShopApiClient; erpClient: ErpRealClient }>({
    shopApiClient: async ({}, use) => {
        await use(new ShopApiClient(config.shop.backendApiUrl));
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
});

// Client fixtures for UI tests
export const uiTest = base.extend<{ shopPage: Page; _shopBrowser: Browser; _shopContext: BrowserContext; shopUiUrl: string; erpClient: ErpRealClient }>({
    shopUiUrl: async ({}, use) => {
        await use(config.shop.frontendUrl);
    },
    _shopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    _shopContext: async ({ _shopBrowser }, use) => {
        const context = await _shopBrowser.newContext({ viewport: { width: 1920, height: 1080 } });
        await use(context);
        await context.close();
    },
    shopPage: async ({ _shopContext }, use) => {
        const page = await _shopContext.newPage();
        await use(page);
        await page.close();
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
});

export { expect } from '@playwright/test';
export { config };
