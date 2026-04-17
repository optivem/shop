import { test, forChannels, ChannelType } from '../fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ scenario }) => {
        await scenario.assume().shop().shouldBeRunning();
    });
});
