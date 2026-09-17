import { test } from '../fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

test.use({ externalSystemMode: 'stub' });

registerErpContractTests(test);
