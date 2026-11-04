// Maintainable contract spec (narrow-integration level) for coupons — same
// interactions as the legacy coupon.integration test, driven through the shared
// test-kit. Co-generates the canonical pact by idempotent merge.
import { describe, it } from 'vitest';
import { integrationHarness } from '../../support';

const { backend, frontend } = integrationHarness();

describe('coupon gateway narrow integration', () => {
  it('browses coupons via the gateway directly', async () => {
    backend.returnsCoupons();

    await frontend.browseCoupons().execute().showsCoupon('SAVE10');
  });

  it('publishes a coupon via the gateway directly', async () => {
    backend.acceptsPublishedCoupon('SAVE10', '0.2');

    await frontend.publishCoupon().withCode('SAVE10').withDiscountRate('0.2').execute().succeeded();
  });
});
