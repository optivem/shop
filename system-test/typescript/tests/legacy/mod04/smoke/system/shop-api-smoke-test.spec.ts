import { apiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToShop', async ({ shopApiClient }) => {
    const result = await shopApiClient.health();
    expect(result.success).toBe(true);
});
