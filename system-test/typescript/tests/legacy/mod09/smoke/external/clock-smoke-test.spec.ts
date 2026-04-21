import { test } from '../../base/BaseScenarioDslTest.js';

test('shouldBeAbleToGoToClock', async ({ scenario }) => {
    await scenario.assume().clock().shouldBeRunning();
});
