import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    test('cannotPlaceOrderWithExpiredCoupon', async ({ scenario }) => {
        await scenario
            .given()
            .clock()
            .withTime('2023-09-01T12:00:00Z')
            .and()
            .coupon()
            .withCouponCode('SUMMER2023')
            .withValidFrom('2023-06-01T00:00:00Z')
            .withValidTo('2023-08-31T23:59:59Z')
            .when()
            .placeOrder()
            .withCouponCode('SUMMER2023')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('couponCode', 'Coupon code SUMMER2023 has expired');
    });

    test('shouldRejectOrderPlacedAtYearEnd', async ({ scenario }) => {
        await scenario
            .given()
            .clock()
            .withTime('2026-12-31T23:59:30Z')
            .when()
            .placeOrder()
            .then()
            .shouldFail()
            .errorMessage('Orders cannot be placed between 23:59 and 00:00 on December 31st');
    });
});
