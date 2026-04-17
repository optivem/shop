import { apiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrder', async ({ config }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;
    const erpBaseUrl = config.externalSystems.erp.url;
    const shopApiUrl = config.shop.backendApiUrl;

    // Given: create product in ERP
    const createProductResponse = await fetch(`${erpBaseUrl}/api/products/${sku}`, { method: 'HEAD' }).catch(() => null);
    await fetch(`${erpBaseUrl}/__admin/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            request: { method: 'GET', urlPath: `/erp/api/products/${sku}` },
            response: { status: 200, headers: { 'Content-Type': 'application/json' }, jsonBody: { id: sku, title: 'Test Product', description: 'Test', price: 20.0, category: 'Test', brand: 'Test' } },
        }),
    });

    // Given: set up tax rate
    await fetch(`${config.externalSystems.tax.url}/__admin/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            request: { method: 'GET', urlPath: `/tax/api/countries/US` },
            response: { status: 200, headers: { 'Content-Type': 'application/json' }, jsonBody: { id: 'US', countryName: 'US', taxRate: 0.07 } },
        }),
    });

    // When: place order via raw HTTP
    const placeOrderResponse = await fetch(`${shopApiUrl}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sku, quantity: '5', country: 'US' }),
    });

    // Then: should succeed
    expect(placeOrderResponse.ok).toBe(true);
    const orderData = await placeOrderResponse.json();
    expect(orderData.orderNumber).toBeDefined();
});
