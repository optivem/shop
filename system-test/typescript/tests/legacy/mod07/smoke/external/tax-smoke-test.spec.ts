import { test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ useCase }) => {
    const result = await useCase.tax().goToTax();
    expect(result.success).toBe(true);
});
