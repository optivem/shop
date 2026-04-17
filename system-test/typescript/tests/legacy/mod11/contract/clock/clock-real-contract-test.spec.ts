process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../fixtures.js';
import { registerClockContractTests } from './BaseClockContractTest.js';

registerClockContractTests(test);
