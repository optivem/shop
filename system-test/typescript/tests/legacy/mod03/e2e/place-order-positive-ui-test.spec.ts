import { uiTest as test, expect } from './fixtures.js';

const TIMEOUT = 30_000;

test('shouldPlaceOrder', async ({ config, shopPage }) => {
    const erpBaseUrl = config.externalSystems.erp.url;
    const shopUiUrl = config.shop.frontendUrl;

    // Given: set up ERP product stub
    await fetch(`${erpBaseUrl}/__admin/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            request: { method: 'GET', urlPath: `/erp/api/products/DEFAULT-SKU` },
            response: { status: 200, headers: { 'Content-Type': 'application/json' }, jsonBody: { id: 'DEFAULT-SKU', title: 'Test Product', description: 'Test', price: 20.0, category: 'Test', brand: 'Test' } },
        }),
    });

    // Given: set up tax rate stub
    await fetch(`${config.externalSystems.tax.url}/__admin/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            request: { method: 'GET', urlPath: `/tax/api/countries/US` },
            response: { status: 200, headers: { 'Content-Type': 'application/json' }, jsonBody: { id: 'US', countryName: 'US', taxRate: 0.07 } },
        }),
    });

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
