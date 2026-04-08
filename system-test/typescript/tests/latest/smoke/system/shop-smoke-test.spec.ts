import { test, forChannels } from '../fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldBeAbleToGoToShop', async ({ scenario }) => {
        await scenario.assume().shop().shouldBeRunning();
    });
});
