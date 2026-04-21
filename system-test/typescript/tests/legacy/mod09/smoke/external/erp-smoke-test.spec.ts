import { test } from '../../base/BaseScenarioDslTest.js';

test('shouldBeAbleToGoToErp', async ({ scenario }) => {
    await scenario.assume().erp().shouldBeRunning();
});
