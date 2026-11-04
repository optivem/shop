// Maintainable contract spec (component level) for PUBLISH COUPON — the frontend twin of the
// system test's latest/acceptance/PublishCouponPositiveTest.
//
// The same interaction the narrow-integration spec drives through the gateway is driven here
// through the admin screen, which is where a coupon is actually published: it pins that the form
// reaches the gateway at all, that the optional fields it leaves empty are OMITTED from the request
// (not sent as nulls), and that a 204 with no body is turned into the confirmation the user reads.
//
// Publishing necessarily reads the coupon table too — the screen hosts both — so the browse
// interaction is staged alongside the publish one.
//
// PublishCouponNegativeTest has no twin here: every one of its rules (discount out of range, zero
// usage limit, duplicate code, blank code) is decided by the backend, and each would render through
// the same errors[] path that place-order.component.test.tsx already pins field by field. The
// acceptance suite owns the rules; this level owns the wiring.
import { describe, it } from 'vitest';
import { componentHarness } from '../../support';

const { backend, frontend } = componentHarness();

describe('PublishCoupon', () => {
  it('confirms the coupon when the backend accepts it', async () => {
    backend.returnsCoupons();
    backend.acceptsPublishedCoupon('SAVE10', '0.2');

    await frontend.publishCoupon().withCode('SAVE10').withDiscountRate('0.2').execute().succeeded();
  });
});
