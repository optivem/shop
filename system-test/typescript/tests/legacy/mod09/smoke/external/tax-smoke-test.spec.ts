import { test } from '../../base/BaseScenarioDslTest.js';

test('shouldBeAbleToGoToTax', async ({ scenario }) => {
    await scenario.assume().tax().shouldBeRunning();
});
