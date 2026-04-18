import { test } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ useCase }) => {
    (await useCase.erp().goToErp().execute()).shouldSucceed();
});
