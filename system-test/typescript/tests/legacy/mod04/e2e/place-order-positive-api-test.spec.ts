import { apiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrderForValidInput', async ({ shopApiClient, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via API client
    const result = await shopApiClient.orders().placeOrder({ sku, quantity: '5', country: 'US' });

    // Then: place order should succeed
    expect(result.success).toBe(true);
    if (result.success) {
        expect(result.value.orderNumber).toMatch(/^ORD-/);

        // Then: view order returns full order details
        const viewResult = await shopApiClient.orders().viewOrder(result.value.orderNumber);
        expect(viewResult.success).toBe(true);
        if (viewResult.success) {
            expect(viewResult.value.orderNumber).toBe(result.value.orderNumber);
            expect(viewResult.value.sku).toBe(sku);
            expect(viewResult.value.quantity).toBe(5);
            expect(viewResult.value.unitPrice).toBe(20);
            expect(viewResult.value.totalPrice).toBeGreaterThan(0);
            expect(viewResult.value.status).toBe('PLACED');
        }
    }
});
