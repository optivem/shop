import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext, Page } from 'playwright';
import { loadConfiguration, type TestConfig } from '../../../../config/configuration-loader.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'real';

const config = loadConfiguration();

// Raw HTTP fixtures for API tests
export const apiTest = base.extend<{ config: TestConfig }>({
    config: async ({}, use) => {
        await use(config);
    },
});

// Raw Playwright fixtures for UI tests
export const uiTest = base.extend<{ config: TestConfig; myShopPage: Page; _myShopBrowser: Browser; _myShopContext: BrowserContext }>({
    config: async ({}, use) => {
        await use(config);
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
