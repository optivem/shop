import { test } from '../fixtures.js';

test('shouldBeAbleToGoToErp', async ({ app }) => {
    (await app.erp().goToErp().execute()).shouldSucceed();
});
