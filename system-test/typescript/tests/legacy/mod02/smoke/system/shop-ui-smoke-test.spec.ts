import { test, expect } from '@playwright/test';
import { chromium } from 'playwright';
import { loadConfiguration } from '../../../../../config/configuration-loader.js';

test('shouldBeAbleToGoToShop', async () => {
    const config = loadConfiguration();

    const browser = await chromium.launch();
    const page = await browser.newPage();

    const response = await page.goto(config.shop.frontendUrl);

    expect(response?.status()).toBe(200);

    const contentType = response?.headers()['content-type'];
    expect(contentType).toBeDefined();
    expect(contentType).toContain('text/html');

    const pageContent = await page.content();
    expect(pageContent).toContain('<html');
    expect(pageContent).toContain('</html>');

    await browser.close();
});
