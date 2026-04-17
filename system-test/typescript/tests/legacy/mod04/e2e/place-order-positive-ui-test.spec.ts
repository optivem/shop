import { uiTest as test, expect } from './fixtures.js';

const TIMEOUT = 30_000;

test('shouldPlaceOrder', async ({ shopPage, shopUiUrl, erpClient, taxClient }) => {
    // Given: set up product and tax via clients
    await erpClient.configureProduct({ sku: 'DEFAULT-SKU', price: '20.00' });
    await taxClient.configureTaxRate({ country: 'US', taxRate: '0.07' });

    // When: place order via UI
    await shopPage.goto(shopUiUrl);
    await shopPage.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
    await shopPage.locator('[aria-label="SKU"]').fill('DEFAULT-SKU', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Quantity"]').fill('5', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Country"]').fill('US', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });

    // Then: should see success notification
    const notification = shopPage.locator("[role='alert'].notification.success");
    await notification.waitFor({ state: 'visible', timeout: TIMEOUT });
    const text = await notification.textContent({ timeout: TIMEOUT });
    expect(text).toContain('Order has been created with Order Number');
});
