import { uiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

const TIMEOUT = 30_000;

test('shouldPlaceOrderForValidInput', async ({ config, shopPage }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;
    const erpBaseUrl = config.externalSystems.erp.url;
    const shopUiUrl = config.shop.frontendUrl;

    // Given: create product in real ERP
    const createProductResponse = await fetch(`${erpBaseUrl}/api/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: sku, title: 'Test Product', description: 'Test', category: 'Test', brand: 'Test', price: '20.00' }),
    });
    expect(createProductResponse.status).toBe(201);

    // When: place order via UI
    await shopPage.goto(shopUiUrl);
    await shopPage.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
    await shopPage.locator('[aria-label="SKU"]').fill(sku, { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Quantity"]').fill('5', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Country"]').fill('US', { timeout: TIMEOUT });
    await shopPage.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });

    // Then: should see success notification with order number
    const notification = shopPage.locator("[role='alert'].notification.success");
    await notification.waitFor({ state: 'visible', timeout: TIMEOUT });
    const successText = await notification.textContent({ timeout: TIMEOUT });
    expect(successText).toContain('Order has been created with Order Number');
    const match = successText?.match(/Order has been created with Order Number ([\w-]+)/);
    expect(match).not.toBeNull();
    const orderNumber = match![1];

    // Then: navigate to order history, filter, view details, assert fields
    await shopPage.goto(`${shopUiUrl}/order-history`);
    await shopPage.locator("[aria-label='Order Number']").fill(orderNumber, { timeout: TIMEOUT });
    await shopPage.locator("[aria-label='Refresh Order List']").click({ timeout: TIMEOUT });
    const row = shopPage.locator(`//tr[contains(., '${orderNumber}')]`);
    await row.locator("//a[contains(text(), 'View Details')]").click({ timeout: TIMEOUT });

    await shopPage.locator("[aria-label='Display Order Number']").waitFor({ state: 'visible', timeout: TIMEOUT });
    const detailsOrderNumber = (await shopPage.locator("[aria-label='Display Order Number']").textContent({ timeout: TIMEOUT }))?.trim();
    expect(detailsOrderNumber).toBe(orderNumber);

    const detailsSku = (await shopPage.locator("[aria-label='Display SKU']").textContent({ timeout: TIMEOUT }))?.trim();
    expect(detailsSku).toBe(sku);

    const quantityText = (await shopPage.locator("[aria-label='Display Quantity']").textContent({ timeout: TIMEOUT }))?.trim() ?? '';
    expect(Number.parseInt(quantityText, 10)).toBe(5);

    const unitPriceText = (await shopPage.locator("[aria-label='Display Unit Price']").textContent({ timeout: TIMEOUT }))?.trim() ?? '';
    expect(Number.parseFloat(unitPriceText.replace('$', ''))).toBe(20);

    const basePriceText = (await shopPage.locator("[aria-label='Display Base Price']").textContent({ timeout: TIMEOUT }))?.trim() ?? '';
    expect(Number.parseFloat(basePriceText.replace('$', ''))).toBe(100);

    const totalPriceText = (await shopPage.locator("[aria-label='Display Total Price']").textContent({ timeout: TIMEOUT }))?.trim() ?? '';
    expect(Number.parseFloat(totalPriceText.replace('$', ''))).toBeGreaterThan(0);

    const status = (await shopPage.locator("[aria-label='Display Status']").textContent({ timeout: TIMEOUT }))?.trim();
    expect(status).toBe('PLACED');
});
