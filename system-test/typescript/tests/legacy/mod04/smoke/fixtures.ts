import { test as base } from '@playwright/test';
import { chromium } from '@playwright/test';
import type { Browser, BrowserContext, Page } from '@playwright/test';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { MyShopApiClient } from '../../../../src/testkit/driver/adapter/api/client/MyShopApiClient.js';
import { ErpRealClient } from '../../../../src/testkit/driver/adapter/external/erp/client/ErpRealClient.js';
import { TaxRealClient } from '../../../../src/testkit/driver/adapter/external/tax/client/TaxRealClient.js';

const config = loadConfiguration();

export const apiTest = base.extend<{ myShopApiClient: MyShopApiClient; erpClient: ErpRealClient; taxClient: TaxRealClient }>({
    myShopApiClient: async ({}, use) => {
        await use(new MyShopApiClient(config.myShop.backendApiUrl));
    },
    erpClient: async ({}, use) => {
        await use(new ErpRealClient(config.externalSystems.erp.url));
    },
    taxClient: async ({}, use) => {
        await use(new TaxRealClient(config.externalSystems.tax.url));
    },
});

export const uiTest = base.extend<{ myShopPage: Page; myShopUiUrl: string; _myShopBrowser: Browser; _myShopContext: BrowserContext }>({
    myShopUiUrl: async ({}, use) => {
        await use(config.myShop.frontendUrl);
    },
    _myShopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    _myShopContext: async ({ _myShopBrowser }, use) => {
        const context = await _myShopBrowser.newContext({ viewport: { width: 1920, height: 1080 } });
        await use(context);
        await context.close();
    },
    myShopPage: async ({ _myShopContext }, use) => {
        const page = await _myShopContext.newPage();
        await use(page);
        await page.close();
    },
});

export { expect } from '@playwright/test';
export { config };
