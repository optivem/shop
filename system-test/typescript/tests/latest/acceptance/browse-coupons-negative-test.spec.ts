import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
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

forChannels(ChannelType.API)(() => {
    test('shouldRejectPageSizeAboveTheCap', async ({ scenario }) => {
        await scenario
            .when()
            .browseCoupons()
            .withSize('101')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('size', 'Page size must be between 1 and 100');
    });

    test('shouldRejectPageBelowTheFirst', async ({ scenario }) => {
        await scenario
            .when()
            .browseCoupons()
            .withPage('0')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('page', 'Page must be 1 or greater');
    });
});
