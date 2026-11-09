import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldSeeOwnOrderInHistory', criteria('UC-002.1', 'UC-002.2'), async ({ scenario }) => {
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
