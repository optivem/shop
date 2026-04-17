import { apiTest as test, expect } from './fixtures.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ shopApiClient }) => {
    // When: place order with non-integer quantity via API client
    const result = await shopApiClient.placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

    // Then: should fail
    expect(result.success).toBe(false);
    if (!result.success) {
        expect(result.error.message).toContain('The request contains one or more validation errors');
        const quantityError = result.error.fieldErrors.find((e) => e.field === 'quantity');
        expect(quantityError?.message).toBe('Quantity must be an integer');
    }
});
