process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

registerErpContractTests(test);
