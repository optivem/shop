import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithInvalidQuantity', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity('invalid-quantity')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be an integer');
    });

    test.eachAlsoFirstRow(['3.5', 'lala'])(
        'shouldRejectOrderWithNonIntegerQuantity_$qty',
        async ({ scenario, qty }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(qty)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must be an integer');
        },
    );

    test('shouldRejectOrderWithNonExistentSku', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withSku('NON-EXISTENT-SKU-12345')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'Product does not exist for SKU: NON-EXISTENT-SKU-12345');
    });

    test.eachAlsoFirstRow(['', '   '])(
        'shouldRejectOrderWithEmptySku_"$sku"',
        async ({ scenario, sku }) => {
            await scenario
                .when()
                .placeOrder()
                .withSku(sku)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('sku', 'SKU must not be empty');
        },
    );

    const nonPositiveQuantities = ['-10', '-1', '0'];

    nonPositiveQuantities.forEach((qty) => {
        test(`shouldRejectOrderWithNonPositiveQuantity_${qty}`, async ({ scenario }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(qty)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must be positive');
        });
    });

    test.eachAlsoFirstRow(['', '   '])(
        'shouldRejectOrderWithEmptyQuantity_"$emptyQuantity"',
        async ({ scenario, emptyQuantity }) => {
            await scenario
                .when()
                .placeOrder()
                .withQuantity(emptyQuantity)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('quantity', 'Quantity must not be empty');
        },
    );

    test.eachAlsoFirstRow(['', '   '])(
        'shouldRejectOrderWithEmptyCountry_"$emptyCountry"',
        async ({ scenario, emptyCountry }) => {
            await scenario
                .when()
                .placeOrder()
                .withCountry(emptyCountry)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage('country', 'Country must not be empty');
        },
    );

    test('shouldRejectOrderWithInvalidCountry', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCountry('XX')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('country', 'Country does not exist: XX');
    });

    test('cannotPlaceOrderWithNonExistentCoupon', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCouponCode('INVALIDCOUPON')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('couponCode', 'Coupon code INVALIDCOUPON does not exist');
    });

    test('cannotPlaceOrderWithCouponThatHasExceededUsageLimit', async ({ scenario }) => {
        await scenario
            .given()
            .coupon()
            .withCouponCode('LIMITED2024')
            .withUsageLimit(2)
            .and()
            .order()
            .withOrderNumber('ORD-1')
            .withCouponCode('LIMITED2024')
            .and()
            .order()
            .withOrderNumber('ORD-2')
            .withCouponCode('LIMITED2024')
            .when()
            .placeOrder()
            .withOrderNumber('ORD-3')
            .withCouponCode('LIMITED2024')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('couponCode', 'Coupon code LIMITED2024 has exceeded its usage limit');
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldRejectOrderWithNullQuantity', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must not be empty');
    });

    test('shouldRejectOrderWithNullSku', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withSku(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'SKU must not be empty');
    });

    test('shouldRejectOrderWithNullCountry', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCountry(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('country', 'Country must not be empty');
    });
});
