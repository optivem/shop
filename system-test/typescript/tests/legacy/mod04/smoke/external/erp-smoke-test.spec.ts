import { apiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ erpClient }) => {
    const result = await erpClient.checkHealth();
    expect(result.success).toBe(true);
});
