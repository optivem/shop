import { test } from '../base/fixtures.js';
import { registerClockContractTests } from './BaseClockContractTest.js';

test.use({ externalSystemMode: 'real' });

registerClockContractTests(test);
