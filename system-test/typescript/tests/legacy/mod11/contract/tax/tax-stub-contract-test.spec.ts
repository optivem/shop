process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../fixtures.js';
import { registerTaxContractTests } from './BaseTaxContractTest.js';

registerTaxContractTests(test);

test('shouldBeAbleToGetConfiguredTaxRate', async ({ scenario }) => {
    await scenario
        .given()
        .country()
        .withCode('LALA')
        .withTaxRate('0.23')
        .then()
        .country('LALA')
        .hasCountry('LALA')
        .hasTaxRate(0.23);
});
