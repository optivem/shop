process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../fixtures.js';
import { registerTaxContractTests } from './BaseTaxContractTest.js';

registerTaxContractTests(test);
