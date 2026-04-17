import { test, expect, forChannels, ChannelType } from '../fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ useCase }) => {
        const result = await useCase.shop().goToShop();
        expect(result.success).toBe(true);
    });
});
