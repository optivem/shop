process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../fixtures.js';
import { registerClockContractTests } from './BaseClockContractTest.js';

registerClockContractTests(test);
