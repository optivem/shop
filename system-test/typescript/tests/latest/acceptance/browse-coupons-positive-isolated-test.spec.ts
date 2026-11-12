import { test, forChannels, ChannelType } from './base/fixtures.js';
import type { ScenarioDsl } from '../../../src/testkit/dsl/scenario-dsl.js';
import { criteria } from '../../commons/criteria.js';

// The coupon list, newest first. The coupons table is shared and never emptied, so these tests check that the
// scenario's coupons come first, in order: true only while no other test publishes a coupon. Not covered: the total
// of 3 and the empty page 3 of UC-005.3, and UC-005.7 (no coupon published), which need a coupon set of their own.
function givenThreeCouponsPublishedInOrder(scenario: ScenarioDsl) {
    return scenario
        .given()
        .coupon()
        .withCouponCode('SUMMER2025')
        .withDiscountRate(0.15)
        .withValidFrom('2025-06-01T00:00:00Z')
        .withValidTo('2025-08-31T23:59:59Z')
        .withUsageLimit(100)
        .and()
        .coupon()
        .withCouponCode('SAVE10')
        .withDiscountRate(0.1)
        .and()
        .coupon()
        .withCouponCode('LIMITED2026')
        .withDiscountRate(0.2)
        .withValidFrom('2026-01-01T00:00:00Z')
        .withUsageLimit(2)
        .and()
        .loggedInAsAdmin();
}

// Page 2 also holds the coupons published before the scenario, after SUMMER2025.
const pageCases = [
    { page: '1', coupons: ['LIMITED2026', 'SAVE10'] },
    { page: '2', coupons: ['SUMMER2025'] },
];

// Checks that its coupons are the newest; a coupon published in parallel would come before them.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldListEveryCouponNewestFirst', criteria('UC-005.1'), async ({ scenario }) => {
            await givenThreeCouponsPublishedInOrder(scenario)
                .when()
                .browseCoupons()
                .then()
                .shouldSucceed()
                .couponList()
                .startsWithCoupons('LIMITED2026', 'SAVE10', 'SUMMER2025');
        });
    });

    forChannels(ChannelType.API)(() => {
        test.each(pageCases)('shouldReadTheCouponsOnePageAtATime_$page', criteria('UC-005.3'), async ({ scenario, page, coupons }) => {
            await givenThreeCouponsPublishedInOrder(scenario)
                .when()
                .browseCoupons()
                .withPage(page)
                .withSize('2')
                .then()
                .shouldSucceed()
                .couponList()
                .startsWithCoupons(...coupons);
        });
    });

    // The UI reads the coupon back from the coupon list, newest first with no paging: coupons published in parallel
    // would push it off the page.
    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldShowHowManyTimesACouponHasBeenUsed', criteria('UC-005.4'), async ({ scenario }) => {
            await scenario
                .given()
                .coupon()
                .withCouponCode('LIMITED2026')
                .withUsageLimit(2)
                .and()
                .order()
                .withOrderNumber('ORD-1')
                .withCouponCode('LIMITED2026')
                .and()
                .order()
                .withOrderNumber('ORD-2')
                .withCouponCode('LIMITED2026')
                .when()
                .browseCoupons()
                .then()
                .shouldSucceed()
                .and()
                .coupon('LIMITED2026')
                .hasUsedCount(2);
        });
    });
});
