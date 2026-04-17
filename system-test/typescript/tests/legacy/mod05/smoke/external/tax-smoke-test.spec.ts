import { apiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ taxDriver }) => {
    const result = await taxDriver.goToTax();
    expect(result.success).toBe(true);
});
