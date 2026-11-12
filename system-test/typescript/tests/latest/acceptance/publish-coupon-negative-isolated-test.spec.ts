import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

// The UI reads the coupon back from the coupon list, newest first with no paging: coupons published in parallel
// would push it off the page.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('cannotPublishCouponWithDuplicateCouponCode', criteria('UC-004.9'), async ({ scenario }) => {
            const dupCode = 'EXISTING-COUPON';
            await scenario
                .given()
                .coupon()
                .withCouponCode(dupCode)
                .withDiscountRate(0.1)
                .when()
                .publishCoupon()
                .withCouponCode(dupCode)
                .withDiscountRate(0.2)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('couponCode', `Coupon code ${dupCode} already exists`)
                .and()
                .coupon(dupCode)
                .hasDiscountRate(0.1);
        });
    });
});
