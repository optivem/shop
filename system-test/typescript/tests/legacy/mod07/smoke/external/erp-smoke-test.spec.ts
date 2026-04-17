import { test, expect } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ useCase }) => {
    const result = await useCase.erp().goToErp();
    expect(result.success).toBe(true);
});
