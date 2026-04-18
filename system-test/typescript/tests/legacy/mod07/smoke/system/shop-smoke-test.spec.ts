import { test, forChannels, ChannelType } from '../fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ app }) => {
        (await app.shop().goToShop().execute()).shouldSucceed();
    });
});
