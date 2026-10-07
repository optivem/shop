import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';

forChannels(ChannelType.API)(() => {
    test('customerShouldNotBeAbleToDeliverOrder', async ({ scenario }) => {
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
