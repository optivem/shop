process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../base/fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

registerErpContractTests(test);
