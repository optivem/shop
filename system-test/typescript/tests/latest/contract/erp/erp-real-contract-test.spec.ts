import { test } from '../base/fixtures.js';
import { registerErpContractTests } from './BaseErpContractTest.js';

test.use({ externalSystemMode: 'real' });

registerErpContractTests(test);
