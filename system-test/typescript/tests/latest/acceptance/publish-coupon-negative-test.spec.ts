import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { emptyArguments } from '../../commons/providers/empty-arguments-provider.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test.eachAlsoFirstRow(['0.0', '-0.01', '-0.15'])(
        'cannotPublishCouponWithZeroOrNegativeDiscount_$discountRate',
        criteria('UC-004.7'),
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
        criteria('UC-004.7'),
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

    test.eachAlsoFirstRow(['0', '-1', '-100'])(
        'cannotPublishCouponWithZeroOrNegativeUsageLimit_$usageLimit',
        criteria('UC-004.8'),
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
    emptyArguments.forEach((code) => {
        test(`shouldRejectCouponWithBlankCode_"${code}"`, criteria('UC-004.6'), async ({ scenario }) => {
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

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldNotBeAbleToPublishCoupon', criteria('UC-004.5'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .publishCoupon()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });
});
