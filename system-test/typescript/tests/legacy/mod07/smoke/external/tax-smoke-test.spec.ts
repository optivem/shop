import { test } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ useCase }) => {
    (await useCase.tax().goToTax().execute()).shouldSucceed();
});
