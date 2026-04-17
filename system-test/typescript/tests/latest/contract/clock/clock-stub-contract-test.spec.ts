process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../base/fixtures.js';
import { registerClockContractTests } from './BaseClockContractTest.js';

registerClockContractTests(test);
