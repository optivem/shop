import { test } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ scenario }) => {
    await scenario.assume().tax().shouldBeRunning();
});
