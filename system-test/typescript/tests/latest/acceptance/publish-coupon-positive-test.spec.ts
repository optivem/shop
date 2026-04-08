import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
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
            .withValidFrom(undefined)
            .withValidTo(undefined)
            .withUsageLimit(undefined)
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
            .shouldSucceed();
    });
});

forChannels('api')(() => {
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
