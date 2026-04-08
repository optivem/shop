import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldBeAbleToBrowseCoupons', async ({ scenario }) => {
        await scenario
            .when()
            .browseCoupons()
            .then()
            .shouldSucceed();
    });

    test('publishedCouponShouldAppearInList', async ({ scenario }) => {
        const couponCode = 'BROWSE10';
        await scenario
            .given()
            .coupon()
            .withCouponCode(couponCode)
            .withDiscountRate(0.1)
            .when()
            .browseCoupons()
            .then()
            .shouldSucceed()
            .coupons()
            .containsCouponWithCode(couponCode);
    });
});
