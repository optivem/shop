import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldPlaceOrder', async ({ scenario }) => {
        await scenario.when().placeOrder().then().shouldSucceed();
    });
});
