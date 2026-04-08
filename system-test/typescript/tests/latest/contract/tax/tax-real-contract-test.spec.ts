process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../base/fixtures.js';

test('shouldBeAbleToGetTaxRate', async ({ scenario }) => {
    await scenario
        .given()
        .country()
        .withCode('US')
        .withTaxRate('0.09')
        .then()
        .country('US')
        .hasTaxRateIsPositive();
});
