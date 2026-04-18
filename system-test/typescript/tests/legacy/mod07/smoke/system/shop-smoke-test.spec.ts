import { test, forChannels, ChannelType } from '../fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToGoToShop', async ({ useCase }) => {
        (await useCase.shop().goToShop().execute()).shouldSucceed();
    });
});
