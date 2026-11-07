import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldPlaceOrder', criteria('UC-001.1'), async ({ scenario }) => {
        await scenario.when().placeOrder().then().shouldSucceed();
    });
});
