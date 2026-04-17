import { apiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ taxClient }) => {
    const result = await taxClient.checkHealth();
    expect(result.success).toBe(true);
});
