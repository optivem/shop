import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToPublishValidCoupon', async ({ scenario }) => {
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

    test('shouldBeAbleToPublishCouponWithEmptyOptionalFields', async ({ scenario }) => {
        await scenario
            .when()
            .publishCoupon()
            .withCouponCode('SUMMER2025')
            .withDiscountRate(0.15)
            .withValidFrom(null)
            .withValidTo(null)
            .withUsageLimit(null)
            .then()
            .shouldSucceed();
    });

    test('shouldBeAbleToCorrectlySaveCoupon', async ({ scenario }) => {
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
