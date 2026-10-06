import { test, forChannels, ChannelType, requiresKeycloak } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';

forChannels(ChannelType.API)(() => {
    test('customerShouldNotBeAbleToDeliverOrder', requiresKeycloak, async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .deliverOrder()
            .withOrderNumber('ORD-NONEXISTENT')
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });
});
