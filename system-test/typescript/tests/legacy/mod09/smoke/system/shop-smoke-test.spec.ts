import { test, forChannels, ChannelType } from '../../base/BaseScenarioDslTest.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ scenario }) => {
        await scenario.assume().shop().shouldBeRunning();
    });
});
