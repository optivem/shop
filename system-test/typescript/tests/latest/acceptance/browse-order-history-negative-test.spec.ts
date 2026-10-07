import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

forChannels(ChannelType.API)(() => {
    test('customerShouldNotSeeAnotherCustomersOrderInHistory', async ({ scenario }) => {
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
});
