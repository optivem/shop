import { test, expect, forChannels, ChannelType } from '../fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ shopDriver }) => {
        const result = await shopDriver.goToShop();
        expect(result.success).toBe(true);
    });
});
