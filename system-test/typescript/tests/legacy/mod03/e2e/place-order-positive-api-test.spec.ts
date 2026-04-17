import { apiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrder', async ({ config }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;
    const erpBaseUrl = config.externalSystems.erp.url;
    const shopApiUrl = config.shop.backendApiUrl;

    // Given: create product in real ERP
    const createProductResponse = await fetch(`${erpBaseUrl}/api/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: sku, title: 'Test Product', description: 'Test', category: 'Test', brand: 'Test', price: '20.00' }),
    });
    expect(createProductResponse.status).toBe(201);

    // When: place order via raw HTTP
    const placeOrderResponse = await fetch(`${shopApiUrl}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sku, quantity: '5', country: 'US' }),
    });

    // Then: should succeed
    expect(placeOrderResponse.ok).toBe(true);
    const orderData = (await placeOrderResponse.json()) as { orderNumber: string };
    expect(orderData.orderNumber).toBeDefined();
});
