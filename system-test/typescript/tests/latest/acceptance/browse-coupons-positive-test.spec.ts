import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToBrowseCoupons', async ({ scenario }) => {
        await scenario
            .when()
            .browseCoupons()
            .then()
            .shouldSucceed();
    });
});

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
