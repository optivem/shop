import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToPublishValidCoupon', criteria('UC-004.2', 'UC-004.3'), async ({ scenario }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SUMMER2025')
            .withDiscountRate(0.15)
            .withValidFrom('2024-06-01T00:00:00Z')
            .withValidTo('2024-08-31T23:59:59Z')
            .withUsageLimit(100)
            .then()
            .shouldSucceed();
    });

    test('shouldBeAbleToPublishCouponWithEmptyOptionalFields', criteria('UC-004.2', 'UC-004.3', 'UC-005.2'), async ({ scenario }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SUMMER2025')
            .withDiscountRate(0.15)
            .withValidFrom(null)
            .withValidTo(null)
            .withUsageLimit(null)
            .then()
            .shouldSucceed()
            .and().coupon('SUMMER2025')
            .isValidImmediately()
            .neverExpires()
            .hasUnlimitedUsage();
    });

    test('shouldBeAbleToCorrectlySaveCoupon', criteria('UC-004.2', 'UC-004.3', 'UC-004.4', 'UC-005.2'), async ({ scenario }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SUMMER2025')
            .withDiscountRate(0.15)
            .withValidFrom('2024-06-01T00:00:00Z')
            .withValidTo('2024-08-31T23:59:00Z')
            .withUsageLimit(100)
            .then()
            .shouldSucceed()
            .and().coupon('SUMMER2025')
            .hasDiscountRate(0.15)
            .isValidFrom('2024-06-01T00:00:00Z')
            .isValidTo('2024-08-31T23:59:00Z')
            .hasUsageLimit(100)
            .hasUsedCount(0);
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldPublishCouponSuccessfully', async ({ scenario }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SAVE10')
            .withDiscountRate(0.1)
            .then()
            .shouldSucceed();
    });
});

test.eachAlsoFirstRow(['0.01', '0.15', '1.00'])(
    'shouldAcceptDiscountRateAboveZeroAndUpToOne_$discountRate',
    criteria('UC-004.1'),
    async ({ scenario, discountRate }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SAVE')
            .withDiscountRate(discountRate)
            .then()
            .shouldSucceed()
            .and().coupon('SAVE')
            .hasDiscountRate(discountRate);
    },
);
