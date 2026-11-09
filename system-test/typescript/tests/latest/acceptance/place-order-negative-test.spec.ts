import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { emptyArguments } from '../../commons/providers/empty-arguments-provider.js';
import { criteria } from '../../commons/criteria.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithInvalidQuantity', criteria('UC-001.8'), async ({ scenario }) => {
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
        criteria('UC-001.8'),
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

    test('shouldRejectOrderWithNonExistentSku', criteria('UC-001.9'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withSku('NON-EXISTENT-SKU-12345')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'Product does not exist for SKU: NON-EXISTENT-SKU-12345');
    });

    test.eachAlsoFirstRow(emptyArguments)(
        'shouldRejectOrderWithEmptySku_"$sku"',
        criteria('UC-001.9'),
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

    test('shouldRejectOrderWithNegativeQuantity', criteria('UC-001.8'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity(-10)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be positive');
    });

    test('shouldRejectOrderWithZeroQuantity', criteria('UC-001.8'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity(0)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be positive');
    });

    test.eachAlsoFirstRow(emptyArguments)(
        'shouldRejectOrderWithEmptyQuantity_"$emptyQuantity"',
        criteria('UC-001.8'),
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

    test.eachAlsoFirstRow(emptyArguments)(
        'shouldRejectOrderWithEmptyCountry_"$emptyCountry"',
        criteria('UC-001.16'),
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

    test('shouldRejectOrderWithInvalidCountry', criteria('UC-001.16'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCountry('XX')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('country', 'Country does not exist: XX');
    });

    test('cannotPlaceOrderWithNonExistentCoupon', criteria('UC-001.11'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withCouponCode('INVALIDCOUPON')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('couponCode', 'Coupon code INVALIDCOUPON does not exist')
            .and()
            .noOrderPersisted();
    });

    test('cannotPlaceOrderWithCouponThatHasExceededUsageLimit', criteria('UC-001.11'), async ({ scenario }) => {
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
    test('shouldRejectOrderWithNullQuantity', criteria('UC-001.8'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must not be empty');
    });

    test('shouldRejectOrderWithNullSku', criteria('UC-001.9'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withSku(null)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'SKU must not be empty');
    });

    test('shouldRejectOrderWithNullCountry', criteria('UC-001.16'), async ({ scenario }) => {
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

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('adminShouldNotBeAbleToPlaceOrder', criteria('UC-001.12'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsAdmin()
            .when()
            .placeOrder()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldRejectReusingAnIdempotencyKeyForADifferentOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withIdempotencyKey('ORDER-KEY')
            .withQuantity('1')
            .and()
            .loggedInAsCustomer()
            .when()
            .placeOrder()
            .withOrderNumber('SECOND-ORDER')
            .withIdempotencyKey('ORDER-KEY')
            .withQuantity('2')
            .then()
            .shouldFail()
            .errorMessage('Idempotency-Key has already been used with a different request');
    });
});

forChannels(ChannelType.API)(() => {
    const incompleteAddressCases = [
        { field: 'firstName', message: 'First name is required' },
        { field: 'lastName', message: 'Last name is required' },
        { field: 'street', message: 'Street is required' },
        { field: 'postalCode', message: 'Postal code is required' },
        { field: 'city', message: 'City is required' },
        { field: 'country', message: 'Country is required' },
    ] as const;

    test.each(incompleteAddressCases)(
        'shouldRejectOrderWithIncompleteShippingAddress_field=$field',
        criteria('UC-001.10'),
        async ({ scenario, field, message }) => {
            const address = {
                firstName: DEFAULTS.FIRST_NAME,
                lastName: DEFAULTS.LAST_NAME,
                street: DEFAULTS.STREET,
                postalCode: DEFAULTS.POSTAL_CODE,
                city: DEFAULTS.CITY,
                country: DEFAULTS.COUNTRY,
                [field]: '',
            };
            await scenario
                .when()
                .placeOrder()
                .withShippingAddress(address.firstName, address.lastName, address.street, address.postalCode, address.city, address.country)
                .then()
                .shouldFail()
                .errorMessage('The request contains one or more validation errors')
                .fieldErrorMessage(`shippingAddress.${field}`, message);
        },
    );

    test('shouldRejectOrderWithShippingAddressFieldOfOnlySpaces', criteria('UC-001.10'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withShippingCity('   ')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('shippingAddress.city', 'City is required');
    });

    test('shouldRejectOrderShippedToUnsupportedCountry', criteria('UC-001.10'), async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withShippingCountry('XX')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('shippingAddress.country', 'Country does not exist: XX');
    });
});
