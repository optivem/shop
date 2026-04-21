import { apiTest as test, expect } from './base/BaseE2eTest.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrderForValidInput', async ({ config }) => {
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

    // Then: place order should succeed
    expect(placeOrderResponse.ok).toBe(true);
    const orderData = (await placeOrderResponse.json()) as { orderNumber: string };
    expect(orderData.orderNumber).toBeDefined();

    // Then: view order via raw HTTP and assert full details
    const viewOrderResponse = await fetch(`${shopApiUrl}/api/orders/${orderData.orderNumber}`);
    expect(viewOrderResponse.ok).toBe(true);
    const order = (await viewOrderResponse.json()) as {
        orderNumber: string;
        sku: string;
        quantity: number;
        unitPrice: number;
        basePrice: number;
        totalPrice: number;
        status: string;
    };
    expect(order.orderNumber).toBe(orderData.orderNumber);
    expect(order.sku).toBe(sku);
    expect(order.quantity).toBe(5);
    expect(order.unitPrice).toBe(20);
    expect(order.basePrice).toBe(100);
    expect(order.totalPrice).toBeGreaterThan(0);
    expect(order.status).toBe('PLACED');
});
