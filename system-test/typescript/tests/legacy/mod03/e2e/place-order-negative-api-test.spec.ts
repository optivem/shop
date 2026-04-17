import { apiTest as test, expect } from './fixtures.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ config }) => {
    const shopApiUrl = config.shop.backendApiUrl;

    // When: place order with invalid quantity via raw HTTP
    const response = await fetch(`${shopApiUrl}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sku: 'SOME-SKU', quantity: 'invalid-quantity', country: 'US' }),
    });

    // Then: should fail with validation error
    expect(response.ok).toBe(false);
    const errorData = await response.json();
    expect(errorData.detail).toContain('The request contains one or more validation errors');
    const quantityError = errorData.errors?.find((e: { field: string }) => e.field === 'quantity');
    expect(quantityError?.message).toBe('Quantity must be an integer');
});
