process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

import { bindChannels, bindTestEach } from '@optivem/optivem-testing';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

const _test = withApp();
const test = Object.assign(_test, {
    each: bindTestEach(_test),
    eachAlsoFirstRow: bindTestEach(_test, [ChannelType.API], [ChannelType.UI]),
});
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
