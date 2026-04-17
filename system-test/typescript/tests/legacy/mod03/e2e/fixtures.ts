import { test as base, expect } from '@playwright/test';
import { chromium } from 'playwright';
import type { Browser, BrowserContext, Page } from 'playwright';
import { loadConfiguration, type TestConfig } from '../../../../config/configuration-loader.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

// Raw HTTP fixtures for API tests
export const apiTest = base.extend<{ config: TestConfig }>({
    config: async ({}, use) => {
        await use(config);
    },
});

// Raw Playwright fixtures for UI tests
export const uiTest = base.extend<{ config: TestConfig; shopPage: Page; _shopBrowser: Browser; _shopContext: BrowserContext }>({
    config: async ({}, use) => {
        await use(config);
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

export { expect, config };
