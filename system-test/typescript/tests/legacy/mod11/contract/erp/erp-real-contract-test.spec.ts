import { test } from '../fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

test.use({ externalSystemMode: 'real' });

registerErpContractTests(test);
