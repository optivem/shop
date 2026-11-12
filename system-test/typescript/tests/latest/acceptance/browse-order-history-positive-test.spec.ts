import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldSeeOwnOrderInHistory', criteria('UC-002.1', 'UC-002.2', 'UC-011.5'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder(DEFAULTS.ORDER_NUMBER);
    });

    test('adminShouldSeeCustomerOrderInHistory', criteria('UC-011.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder(DEFAULTS.ORDER_NUMBER);
    });

    test('adminShouldSeeAnotherCustomersOrderInHistory', criteria('UC-011.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder(DEFAULTS.ORDER_NUMBER);
    });

    test('adminShouldSeeOrderOfChosenCustomer', criteria('UC-011.3', 'UC-011.4'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-A')
            .placedByCustomer('A')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withCustomer('A')
            .withOrderNumber('ORD-A')
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder('ORD-A');
    });

    // Not covered: the "newest first" order of @UC-011.1 (the given orders cannot be dated) and @UC-011.2 (an order
    // with no recorded customer cannot be placed).
    test('shouldShowTheDetailsOfEachOrderInTheList', criteria('UC-011.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-B')
            .withQuantity(2)
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .showsOrder('ORD-B', 'B', DEFAULTS.SKU, 2, OrderStatus.PLACED);
    });

    test('shouldShowOnlyTheOrdersOfTheChosenCustomer', criteria('UC-011.3'), async ({ scenario }) => {
        // The chosen customer is a new one: a shared customer has more orders than fit on page 1, all at the same stub
        // clock time, so its new order would not reliably be on the first page.
        await scenario
            .given()
            .newCustomer()
            .and()
            .order()
            .withOrderNumber('ORD-A')
            .placedByCustomer()
            .and()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withCustomer()
            .withNoOrderNumberFilter()
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder('ORD-A')
            .doesNotContainOrder('ORD-B');
    });
});

forChannels(ChannelType.API)(() => {
    test('firstPageShouldReportTheTotalNumberOfMatchingOrders', criteria('UC-002.3'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .withPage('1')
            .withSize('1')
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder(DEFAULTS.ORDER_NUMBER)
            .hasTotalElements(1);
    });

    test('pagePastTheLastShouldBeEmpty', criteria('UC-002.3'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .withPage('2')
            .then()
            .shouldSucceed()
            .orderHistory()
            .isEmpty();
    });
});

forChannels(ChannelType.API)(() => {
    test('customerFilterShouldBeIgnoredForCustomer', criteria('UC-011.5'), async ({ scenario }) => {
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
            .withOrderNumber('ORD-A')
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder('ORD-A');
    });
});

forChannels(ChannelType.UI)(() => {
    // Opening the linked order is view-order-positive-test's adminShouldBeAbleToViewAnotherCustomersOrder.
    test('shouldLinkEachOrderToItsDetails', criteria('UC-011.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseOrderHistory()
            .withOrderNumber('ORD-B')
            .then()
            .shouldSucceed()
            .orderHistory()
            .linksOrderToItsDetails('ORD-B');
    });

    test('shouldOfferTheCustomersWhoHavePlacedOrders', criteria('UC-011.3'), async ({ scenario }) => {
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
            .withNoOrderNumberFilter()
            .then()
            .shouldSucceed()
            .orderHistory()
            .offersCustomers('A', 'B')
            .hasCustomerFilterSetToAllCustomers();
    });

    test('shouldNotShowACustomerTheCustomerColumnOrTheCustomerFilter', criteria('UC-011.7'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .containsOrder(DEFAULTS.ORDER_NUMBER)
            .showsNoCustomers()
            .offersNoCustomerFilter();
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldShowTheFirstPageOf50OrdersByDefault', criteria('UC-002.3'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .withNoOrderNumberFilter()
            .then()
            .shouldSucceed()
            .orderHistory()
            .hasPage(1)
            .hasPageSize(50);
    });
});

forChannels(ChannelType.UI)(() => {
    // Opening the linked order is view-order-positive-test's customerShouldBeAbleToViewOwnOrder.
    test('shouldLinkEachOfTheCustomersOrdersToItsDetails', criteria('UC-002.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .browseOrderHistory()
            .then()
            .shouldSucceed()
            .orderHistory()
            .linksOrderToItsDetails(DEFAULTS.ORDER_NUMBER);
    });
});
