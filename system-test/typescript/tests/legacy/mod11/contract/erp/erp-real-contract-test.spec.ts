process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

registerErpContractTests(test);
