import { apiTest as test, expect } from './base/BaseE2eTest.js';
import { authHeadersFor } from '../../../../src/testkit/driver/adapter/shared/client/http/keycloak-token-provider.js';
import { TestUsers } from '../../../../src/testkit/driver/adapter/shared/client/http/test-user.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ config }) => {
    const myShopApiUrl = config.myShop.backendApiUrl;

    // When: place order with invalid quantity via raw HTTP
    const customerAuth = await authHeadersFor(config.keycloakUrl, TestUsers.CUSTOMER);
    const response = await fetch(`${myShopApiUrl}/api/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...customerAuth },
        body: JSON.stringify({ sku: 'SOME-SKU', quantity: 'invalid-quantity', country: 'US' }),
    });

    // Then: should fail with validation error
    expect(response.ok).toBe(false);
    const errorData = (await response.json()) as { detail: string; errors?: { field: string; message: string }[] };
    expect(errorData.detail).toContain('The request contains one or more validation errors');
    const quantityError = errorData.errors?.find((e) => e.field === 'quantity');
    expect(quantityError?.message).toBe('Quantity must be an integer');
});
