import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test.eachAlsoFirstRow(['0.0', '-0.01', '-0.15'])(
        'cannotPublishCouponWithZeroOrNegativeDiscount_$discountRate',
        async ({ scenario, discountRate }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID-COUPON')
                .withDiscountRate(discountRate)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('discountRate', 'Discount rate must be greater than 0.00');
        },
    );

    test.eachAlsoFirstRow(['1.01', '2.00'])(
        'cannotPublishCouponWithDiscountGreaterThan100percent_$discountRate',
        async ({ scenario, discountRate }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID-COUPON')
                .withDiscountRate(discountRate)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('discountRate', 'Discount rate must be at most 1.00');
        },
    );

    test('cannotPublishCouponWithDuplicateCouponCode', async ({ scenario }) => {
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
            .fieldErrorMessage('couponCode', `Coupon code ${dupCode} already exists`);
    });

    test.eachAlsoFirstRow([0, -1, -100])(
        'cannotPublishCouponWithZeroOrNegativeUsageLimit_$usageLimit',
        async ({ scenario, usageLimit }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID-LIMIT')
                .withDiscountRate(0.15)
                .withUsageLimit(usageLimit)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('usageLimit', 'Usage limit must be positive');
        },
    );
});

forChannels(ChannelType.API)(() => {
    const emptyCodes = ['', '   '];

    emptyCodes.forEach((code) => {
        test(`shouldRejectCouponWithBlankCode_"${code}"`, async ({ scenario }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode(code)
                .withDiscountRate(0.1)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('code', 'Coupon code must not be blank');
        });
    });
});
