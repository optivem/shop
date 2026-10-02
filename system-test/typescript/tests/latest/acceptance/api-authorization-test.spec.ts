import { test as base, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { loadConfiguration } from '../../../config/configuration-loader.js';
import { MyShopApiClient } from '../../../src/testkit/driver/adapter/api/client/MyShopApiClient.js';
import { ApiIdentity } from '../../../src/testkit/driver/adapter/api/client/api-identity.js';
import { assertThatResult } from '../../../src/testkit/common/result-assert.js';

// Authorization is checked at the API client level (who may call what), not through the DSL.
// Runs only when a Keycloak URL is configured (KEYCLOAK_URL_STUB / KEYCLOAK_URL).
const config = loadConfiguration({ externalSystemMode: 'stub' });

const test = base.extend<{ myShopApiClient: MyShopApiClient }>({
  myShopApiClient: async ({}, use) => {
    await use(new MyShopApiClient(config.myShop.backendApiUrl, config.keycloakUrl));
  },
});

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

test('shouldRejectCustomerBrowsingCoupons', async ({ myShopApiClient }) => {
  const result = await myShopApiClient.as(ApiIdentity.CUSTOMER).coupons().browseCoupons();

  expect(assertThatResult(result).isFailure().getError().status).toBe(403);
});

test('shouldAllowAdminToBrowseCoupons', async ({ myShopApiClient }) => {
  const result = await myShopApiClient.as(ApiIdentity.ADMIN).coupons().browseCoupons();

  assertThatResult(result).isSuccess();
});

test('shouldRejectCustomerPublishingCoupon', async ({ myShopApiClient }) => {
  const result = await myShopApiClient.as(ApiIdentity.CUSTOMER).coupons().publishCoupon(newCouponRequest());

  expect(assertThatResult(result).isFailure().getError().status).toBe(403);
});

test('shouldRejectCustomerDeliveringOrder', async ({ myShopApiClient }) => {
  const result = await myShopApiClient.as(ApiIdentity.CUSTOMER).orders().deliverOrder('ORD-NONEXISTENT');

  expect(assertThatResult(result).isFailure().getError().status).toBe(403);
});

test('shouldAllowAdminToPublishCoupon', async ({ myShopApiClient }) => {
  const result = await myShopApiClient.as(ApiIdentity.ADMIN).coupons().publishCoupon(newCouponRequest());

  assertThatResult(result).isSuccess();
});
