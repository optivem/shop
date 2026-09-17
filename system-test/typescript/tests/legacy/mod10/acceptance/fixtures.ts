import { bindChannels } from '@optivem/optivem-testing';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';

const _test = withApp('stub');
const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { expect } from '@playwright/test';
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
