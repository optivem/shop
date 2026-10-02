import { apiTest as test, expect } from './base/BaseE2eTest.js';
import { randomUUID } from 'node:crypto';
import { authHeadersFor } from '../../../../src/testkit/driver/adapter/shared/client/http/keycloak-token-provider.js';
import { TestUsers } from '../../../../src/testkit/driver/adapter/shared/client/http/test-user.js';

test('shouldPlaceOrderForValidInput', async ({ config }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;
    const erpBaseUrl = config.externalSystems.erp.url;
    const myShopApiUrl = config.myShop.backendApiUrl;

    // Given: create product in real ERP
    const createProductResponse = await fetch(`${erpBaseUrl}/api/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: sku, title: 'Test Product', description: 'Test', category: 'Test', brand: 'Test', price: '20.00' }),
    });
    expect(createProductResponse.status).toBe(201);

    // When: place order via raw HTTP
    const customerAuth = await authHeadersFor(config.keycloakUrl, TestUsers.CUSTOMER);
    const placeOrderResponse = await fetch(`${myShopApiUrl}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...customerAuth },
        body: JSON.stringify({ sku, quantity: '5', country: 'US' }),
    });

    // Then: place order should succeed
    expect(placeOrderResponse.ok).toBe(true);
    const orderData = (await placeOrderResponse.json()) as { orderNumber: string };
    expect(orderData.orderNumber).toBeDefined();

    // Then: view order via raw HTTP and assert full details
    const viewOrderResponse = await fetch(`${myShopApiUrl}/api/orders/${orderData.orderNumber}`, { headers: customerAuth });
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
