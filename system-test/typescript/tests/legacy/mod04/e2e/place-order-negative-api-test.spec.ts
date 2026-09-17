import { apiTest as test, expect } from './base/BaseE2eTest.js';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ myShopApiClient }) => {
    // When: place order with non-integer quantity via API client
    const result = await myShopApiClient.orders().placeOrder({ sku: 'SOME-SKU', quantity: 'invalid-quantity', country: 'US' });

    // Then: should fail
    expect(result.success).toBe(false);
    const error = assertThatResult(result).getError();
    expect(error.message).toContain('The request contains one or more validation errors');
    const quantityError = error.fieldErrors.find((e) => e.field === 'quantity');
    expect(quantityError?.message).toBe('Quantity must be an integer');
});
