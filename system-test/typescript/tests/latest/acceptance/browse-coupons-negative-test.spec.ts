import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';

forChannels(ChannelType.API)(() => {
    test('customerShouldNotBeAbleToBrowseCoupons', async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsCustomer()
            .when()
            .browseCoupons()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED);
    });
});
