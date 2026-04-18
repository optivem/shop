process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../fixtures.js';
import { registerTaxContractTests } from './BaseTaxContractTest.js';

registerTaxContractTests(test);
