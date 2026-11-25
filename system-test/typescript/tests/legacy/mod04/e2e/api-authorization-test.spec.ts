import { apiTest as test, expect, config } from './base/BaseE2eTest.js';
import { randomUUID } from 'node:crypto';
import { ApiIdentity } from '../../../../src/testkit/driver/adapter/api/client/api-identity.js';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';
import { KeycloakIdentityProviderDriver } from '../../../../src/testkit/driver/adapter/external/identity/keycloak-identity-provider-driver.js';
import { testUserOf } from '../../../../src/testkit/driver/adapter/shared/client/http/test-user.js';
import { UserIdentity } from '../../../../src/testkit/driver/port/user-identity.js';

// Runs only when a Keycloak URL is configured (KEYCLOAK_URL_REAL / KEYCLOAK_URL).
test.skip(!config.keycloakUrl, 'Authorization tests require KEYCLOAK_URL to be set');

function newCouponRequest() {
    return { code: `AUTH-${randomUUID().substring(0, 8).toUpperCase()}`, discountRate: '0.10' };
}

test('shouldNotRequireTokenForHealth', async ({ myShopApiClient }) => {
    const result = await myShopApiClient.as(ApiIdentity.ANONYMOUS).health().checkHealth();

    assertThatResult(result).isSuccess();
});

test('shouldRejectRequestWithoutToken', async ({ myShopApiClient }) => {
    const result = await myShopApiClient.as(ApiIdentity.ANONYMOUS).orders().viewOrder('ORD-NONEXISTENT');

    expect(assertThatResult(result).isFailure().getError().status).toBe(401);
});

test('shouldRejectCustomerPublishingCoupon', async ({ myShopApiClient }) => {
    const result = await myShopApiClient.as(ApiIdentity.CUSTOMER).coupons().publishCoupon(newCouponRequest());

    expect(assertThatResult(result).isFailure().getError().status).toBe(403);
});

test('shouldAllowAdminToPublishCoupon', async ({ myShopApiClient }) => {
    const identityProvider = new KeycloakIdentityProviderDriver(config.keycloakUrl);
    try {
        const admin = UserIdentity.admin(await identityProvider.createAdmin('Test', 'Admin'));

        const result = await myShopApiClient.asUser(testUserOf(admin)).coupons().publishCoupon(newCouponRequest());

        assertThatResult(result).isSuccess();
    } finally {
        await identityProvider.close();
    }
});
