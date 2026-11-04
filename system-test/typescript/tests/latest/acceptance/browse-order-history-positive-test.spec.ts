import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldSeeOwnOrderInHistory', async ({ scenario }) => {
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

    test('adminShouldSeeCustomerOrderInHistory', async ({ scenario }) => {
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

    test('adminShouldSeeAnotherCustomersOrderInHistory', async ({ scenario }) => {
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
});

forChannels(ChannelType.API)(() => {
    test('firstPageShouldReportTheTotalNumberOfMatchingOrders', async ({ scenario }) => {
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

    test('pagePastTheLastShouldBeEmpty', async ({ scenario }) => {
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
