import { bindChannels } from '@optivem/optivem-testing';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

const keycloakUrl = loadConfiguration({ externalSystemMode: 'stub' }).keycloakUrl;
if (!keycloakUrl) {
    throw new Error(
        'Keycloak base URL is not configured. Set the environment variable KEYCLOAK_URL_STUB (or KEYCLOAK_URL) ' +
            'to the Keycloak base URL, e.g. http://localhost:8180, and start the Keycloak container before running these tests.',
    );
}

const _test = withApp('stub');
const test = Object.assign(_test, {
    each: bindTestEach(_test),
    eachAlsoFirstRow: bindTestEach(_test, [ChannelType.API], [ChannelType.UI]),
});
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
