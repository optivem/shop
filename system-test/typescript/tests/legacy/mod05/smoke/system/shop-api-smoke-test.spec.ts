import { apiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToShop', async ({ shopDriver }) => {
    const result = await shopDriver.goToShop();
    expect(result.success).toBe(true);
});
