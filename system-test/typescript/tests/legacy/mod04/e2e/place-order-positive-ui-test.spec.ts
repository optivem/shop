import { uiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

const TIMEOUT = 30_000;

test('shouldPlaceOrder', async ({ shopPage, shopUiUrl, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via UI
    await shopPage.goto(shopUiUrl);
    await shopPage.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
    await shopPage.locator('[aria-label="SKU"]').fill(sku, { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Quantity"]').fill('5', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Country"]').fill('US', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });

    // Then: should see success notification
    const notification = shopPage.locator("[role='alert'].notification.success");
    await notification.waitFor({ state: 'visible', timeout: TIMEOUT });
    const text = await notification.textContent({ timeout: TIMEOUT });
    expect(text).toContain('Order has been created with Order Number');
});
