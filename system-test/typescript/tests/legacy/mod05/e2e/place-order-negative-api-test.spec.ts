import { apiTest as test, expect } from './fixtures.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ shopDriver }) => {
    // When: place order with non-integer quantity via shop driver
    const result = await shopDriver.placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

    // Then
    expect(result.success).toBe(false);
    if (!result.success) {
        expect(result.error.message).toContain('The request contains one or more validation errors');
        const quantityError = result.error.fieldErrors.find((e) => e.field === 'quantity');
        expect(quantityError?.message).toBe('Quantity must be an integer');
    }
});
