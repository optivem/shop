import { bindChannels } from '@optivem/optivem-testing';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

const _test = withApp('stub');
const test = Object.assign(_test, {
    each: bindTestEach(_test),
    eachAlsoFirstRow: bindTestEach(_test, [ChannelType.API], [ChannelType.UI]),
});
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
