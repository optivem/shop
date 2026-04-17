import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToViewOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });
});
