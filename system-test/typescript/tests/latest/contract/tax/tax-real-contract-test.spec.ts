import { test } from '../base/fixtures.js';
import { registerTaxContractTests } from './BaseTaxContractTest.js';

test.use({ externalSystemMode: 'real' });

registerTaxContractTests(test);
