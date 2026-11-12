import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldShowNoOrdersWhenNoOrderMatchesTheFilter', criteria('UC-002.9'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .withOrderNumber('NON-EXISTENT-ORDER-99999')
            .then()
            .shouldSucceed()
            .orderHistory()
            .isEmpty();
    });

    test('customerShouldNotSeeAnotherCustomersOrderInHistory', criteria('UC-002.6', 'UC-011.5'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('A')
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .doesNotContainOrder(DEFAULTS.ORDER_NUMBER);
    });

    test('adminCustomerFilterShouldExcludeOtherCustomersOrders', criteria('UC-011.3'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-A')
            .placedByCustomer('A')
            .and()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withCustomer('A')
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .doesNotContainOrder('ORD-B');
    });

    test('adminShouldSeeNoOrdersWhenNoOrderMatchesFilters', criteria('UC-011.8'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-A')
            .placedByCustomer('A')
            .and()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withCustomer('A')
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .isEmpty();
    });

    test('shouldAskAnAnonymousVisitorToSignIn', criteria('UC-011.6'), async ({ scenario }) => {
        await scenario
            .given()
            .notLoggedIn()
            .when()
            .browseOrderHistory()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.AUTHENTICATION_REQUIRED);
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldRejectPageSizeAboveTheCap', criteria('UC-002.8'), async ({ scenario }) => {
        await scenario
            .when()
            .browseOrderHistory()
            .withSize('101')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('size', 'Page size must be between 1 and 100');
    });

    test('shouldRejectPageBelowTheFirst', criteria('UC-002.8'), async ({ scenario }) => {
        await scenario
            .when()
            .browseOrderHistory()
            .withPage('0')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('page', 'Page must be 1 or greater');
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldRejectPageSizeBelowOne', criteria('UC-002.8'), async ({ scenario }) => {
        await scenario
            .when()
            .browseOrderHistory()
            .withSize('0')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('size', 'Page size must be between 1 and 100');
    });
});

forChannels(ChannelType.API)(() => {
    // API only: a customer's order history has no customer filter to choose another customer with.
    test('customerShouldNotSeeAnotherCustomersOrdersThroughCustomerFilter', criteria('UC-011.5'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-A')
            .placedByCustomer('A')
            .and()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('A')
            .when()
            .browseOrderHistory()
            .withCustomer('B')
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .doesNotContainOrder('ORD-B');
    });
});

forChannels(ChannelType.UI)(() => {
    // Not covered: the @UC-011.5 order with no recorded customer (such an order cannot be placed).
    test('shouldTellTheAdminWhenNoOrderMatchesTheFilters', criteria('UC-011.8'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withCustomer('A')
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .isEmpty()
            .showsMessage('No orders found');
    });

    test('shouldTellTheCustomerWhenNoOrderMatchesTheFilter', criteria('UC-002.9'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .withOrderNumber('NON-EXISTENT-ORDER-99999')
            .then()
            .shouldSucceed()
            .orderHistory()
            .isEmpty()
            .showsMessage('No orders found');
    });
});
