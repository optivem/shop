process.env.EXTERNAL_SYSTEM_MODE = envOrDefault('EXTERNAL_SYSTEM_MODE', 'stub');

import { bindChannels } from '@optivem/optivem-testing';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
import { envOrDefault } from '../../../../src/testkit/common/fallback.js';

const _test = withApp();
const test = Object.assign(_test, {
    each: bindTestEach(_test),
    eachAlsoFirstRow: bindTestEach(_test, [ChannelType.API], [ChannelType.UI]),
});
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
