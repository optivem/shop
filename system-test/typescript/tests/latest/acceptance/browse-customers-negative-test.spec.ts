import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { criteria } from '../../commons/criteria.js';

// API only: the UI has no customer list of its own (see browse-customers-positive-test).
forChannels(ChannelType.API)(() => {
    test('customerShouldNotBeAbleToBrowseCustomers', criteria('UC-011.5'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .browseCustomers()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });

    test('anonymousShouldNotBeAbleToBrowseCustomers', criteria('UC-011.6'), async ({ scenario }) => {
        await scenario
            .given()
            .notLoggedIn()
            .when()
            .browseCustomers()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.AUTHENTICATION_REQUIRED);
    });
});
