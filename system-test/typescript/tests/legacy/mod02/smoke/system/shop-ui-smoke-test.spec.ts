import { test, expect } from '@playwright/test';
import { getShopUiBaseUrl, setUpShopBrowser, tearDownShopBrowser, type ShopBrowser } from '../../base/BaseRawTest.js';

test('shouldBeAbleToGoToShop', async () => {
    let shopBrowser: ShopBrowser | null = null;
    try {
        shopBrowser = await setUpShopBrowser();
        const response = await shopBrowser.page.goto(getShopUiBaseUrl());

        expect(response?.status()).toBe(200);

        const contentType = response?.headers()['content-type'];
        expect(contentType).toBeDefined();
        expect(contentType).toContain('text/html');

        const pageContent = await shopBrowser.page.content();
        expect(pageContent).toContain('<html');
        expect(pageContent).toContain('</html>');
    } finally {
        await tearDownShopBrowser(shopBrowser);
    }
});
