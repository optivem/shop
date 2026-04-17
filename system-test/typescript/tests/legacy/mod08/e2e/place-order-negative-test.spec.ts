import { test, forChannels, ChannelType } from './fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithNonIntegerQuantity', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .withQuantity('3.5')
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be an integer');
    });
});
