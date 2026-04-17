import { uiTest as test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToShop', async ({ shopPage, shopUiUrl }) => {
    const response = await shopPage.goto(shopUiUrl);
    expect(response?.status()).toBe(200);
});
