import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    const nonPositiveDiscountRates = [0.0, -0.01, -0.15];

    nonPositiveDiscountRates.forEach((discountRate) => {
        test(`cannotPublishCouponWithZeroOrNegativeDiscount_${discountRate}`, async ({ scenario }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID')
                .withDiscountRate(discountRate)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('discountRate', 'Discount rate must be greater than 0.00');
        });
    });

    const aboveOneDiscountRates = [1.01, 2.0];

    aboveOneDiscountRates.forEach((discountRate) => {
        test(`cannotPublishCouponWithDiscountGreaterThan100percent_${discountRate}`, async ({ scenario }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID')
                .withDiscountRate(discountRate)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('discountRate', 'Discount rate must be at most 1.00');
        });
    });

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

    const nonPositiveUsageLimits = [0, -1, -100];

    nonPositiveUsageLimits.forEach((usageLimit) => {
        test(`cannotPublishCouponWithZeroOrNegativeUsageLimit_${usageLimit}`, async ({ scenario }) => {
            await scenario
                .when()
                .publishCoupon()
                .withCouponCode('INVALID-LIMIT')
                .withDiscountRate(0.1)
                .withUsageLimit(usageLimit)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('usageLimit', 'Usage limit must be positive');
        });
    });
});

forChannels('api')(() => {
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
