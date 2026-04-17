import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { ShopApiClient } from '../../../../src/testkit/driver/adapter/shop/api/client/ShopApiClient.js';
import { ShopUiClient } from '../../../../src/testkit/driver/adapter/shop/ui/client/ShopUiClient.js';
import { ErpRealClient } from '../../../../src/testkit/driver/adapter/external/erp/client/ErpRealClient.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE ?? 'real';

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
export const uiTest = base.extend<{ shopUiClient: ShopUiClient; _shopBrowser: Browser; erpClient: ErpRealClient }>({
    _shopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    shopUiClient: async ({ _shopBrowser }, use) => {
        const client = new ShopUiClient(config.shop.frontendUrl, _shopBrowser);
        await use(client);
        await client.close();
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
});

export { expect } from '@playwright/test';
export { config };
