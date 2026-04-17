import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext, Page } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { ShopApiClient } from '../../../../src/testkit/driver/adapter/shop/api/client/ShopApiClient.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

export const apiTest = base.extend<{ shopApiClient: ShopApiClient }>({
    shopApiClient: async ({}, use) => {
        await use(new ShopApiClient(config.shop.backendApiUrl));
    },
});

export const uiTest = base.extend<{ shopPage: Page; shopUiUrl: string; _shopBrowser: Browser; _shopContext: BrowserContext }>({
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
});

export { expect } from '@playwright/test';
export { config };
