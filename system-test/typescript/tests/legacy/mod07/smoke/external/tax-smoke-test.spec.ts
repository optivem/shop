import { test } from '../fixtures.js';

test('shouldBeAbleToGoToTax', async ({ app }) => {
    (await app.tax().goToTax().execute()).shouldSucceed();
});
