import { test } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ scenario }) => {
    await scenario.assume().erp().shouldBeRunning();
});
