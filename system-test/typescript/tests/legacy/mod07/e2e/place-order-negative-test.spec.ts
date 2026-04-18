import { test, forChannels, ChannelType } from './fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithNonIntegerQuantity', async ({ app }) => {
        (await app.shop().placeOrder()
            .sku('SOME-SKU').quantity('3.5').country('US').execute())
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be an integer');
    });
});
