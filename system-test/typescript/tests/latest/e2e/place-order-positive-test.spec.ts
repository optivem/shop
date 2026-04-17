import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldPlaceOrder', async ({ scenario }) => {
        await scenario.when().placeOrder().then().shouldSucceed();
    });
});
