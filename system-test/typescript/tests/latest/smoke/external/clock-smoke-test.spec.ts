import { test } from '../fixtures.js';

test('shouldBeAbleToGoToClock', async ({ scenario }) => {
    await scenario.assume().clock().shouldBeRunning();
});
