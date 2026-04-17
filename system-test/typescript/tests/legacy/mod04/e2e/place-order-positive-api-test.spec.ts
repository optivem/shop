import { apiTest as test, expect } from './fixtures.js';
import { randomUUID } from 'node:crypto';

test('shouldPlaceOrder', async ({ shopApiClient, erpClient, taxClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: set up product and tax via clients
    await erpClient.configureProduct({ sku, price: '20.00' });
    await taxClient.configureTaxRate({ country: 'US', taxRate: '0.07' });

    // When: place order via API client
    const result = await shopApiClient.placeOrder({ sku, quantity: '5', country: 'US' });

    // Then
    expect(result.success).toBe(true);
    if (result.success) {
        expect(result.value.orderNumber).toBeDefined();
    }
});
