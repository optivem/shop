import { test, forChannels, ChannelType } from './base/fixtures.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
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

forChannels(ChannelType.API)(() => {
    test('shouldRejectPageSizeAboveTheCap', async ({ scenario }) => {
        await scenario
            .when()
            .browseOrderHistory()
            .withSize('101')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('size', 'Page size must be between 1 and 100');
    });

    test('shouldRejectPageBelowTheFirst', async ({ scenario }) => {
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
