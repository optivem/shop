import { test } from '../fixtures.js';
import { registerClockContractTests } from './BaseClockContractTest.js';

test.use({ externalSystemMode: 'stub' });

registerClockContractTests(test);
