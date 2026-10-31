import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldNotBeAbleToDeliverOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .deliverOrder()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });
});
