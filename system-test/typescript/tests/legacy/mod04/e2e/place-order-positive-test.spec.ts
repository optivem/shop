import { test, forChannels } from './fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldPlaceOrder', async ({ scenario }) => {
        await scenario.when().placeOrder().then().shouldSucceed();
    });
});
