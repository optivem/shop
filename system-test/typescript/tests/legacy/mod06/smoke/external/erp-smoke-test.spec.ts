import { test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ erpDriver }) => {
    const result = await erpDriver.goToErp();
    expect(result.success).toBe(true);
});
