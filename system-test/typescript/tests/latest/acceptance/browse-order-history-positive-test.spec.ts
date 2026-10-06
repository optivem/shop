import { test, forChannels, ChannelType, requiresKeycloak } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

forChannels(ChannelType.API)(() => {
    test('customerShouldSeeOwnOrderInHistory', requiresKeycloak, async ({ scenario }) => {
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

    test('adminShouldSeeCustomerOrderInHistory', requiresKeycloak, async ({ scenario }) => {
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

    test('adminShouldSeeAnotherCustomersOrderInHistory', requiresKeycloak, async ({ scenario }) => {
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
