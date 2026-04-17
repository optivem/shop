process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../base/fixtures.js';
import { registerTaxContractTests } from './BaseTaxContractTest.js';

registerTaxContractTests(test);
