import { apiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrderForValidInput', async ({ shopApiClient, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via API client
    const result = await shopApiClient.orders().placeOrder({ sku, quantity: '5', country: 'US' });

    // Then
    expect(result.success).toBe(true);
    if (result.success) {
        expect(result.value.orderNumber).toBeDefined();
    }
});
