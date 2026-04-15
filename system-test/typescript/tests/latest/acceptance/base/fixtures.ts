process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

import { bindChannels, bindTestEach } from '@optivem/optivem-testing';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/playwright/withApp.js';

const _test = withApp();
const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { expect } from '@playwright/test';
