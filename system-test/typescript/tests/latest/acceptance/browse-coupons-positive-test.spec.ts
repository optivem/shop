import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToBrowseCoupons', async ({ scenario }) => {
        await scenario
            .when()
            .browseCoupons()
            .then()
            .shouldSucceed();
    });
});
