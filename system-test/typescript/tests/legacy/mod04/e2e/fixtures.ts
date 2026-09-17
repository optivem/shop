import { test as base } from '@playwright/test';
import { chromium } from '@playwright/test';
import type { Browser } from '@playwright/test';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { MyShopApiClient } from '../../../../src/testkit/driver/adapter/api/client/MyShopApiClient.js';
import { MyShopUiClient } from '../../../../src/testkit/driver/adapter/ui/client/MyShopUiClient.js';
import { ErpRealClient } from '../../../../src/testkit/driver/adapter/external/erp/client/ErpRealClient.js';

const config = loadConfiguration();

// Client fixtures for API tests
export const apiTest = base.extend<{ myShopApiClient: MyShopApiClient; erpClient: ErpRealClient }>({
    myShopApiClient: async ({}, use) => {
        await use(new MyShopApiClient(config.myShop.backendApiUrl));
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
});

// Client fixtures for UI tests
export const uiTest = base.extend<{ myShopUiClient: MyShopUiClient; _myShopBrowser: Browser; erpClient: ErpRealClient }>({
    _myShopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    myShopUiClient: async ({ _myShopBrowser }, use) => {
        const client = new MyShopUiClient(config.myShop.frontendUrl, _myShopBrowser);
        await use(client);
        await client.close();
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
});

export { expect } from '@playwright/test';
export { config };
