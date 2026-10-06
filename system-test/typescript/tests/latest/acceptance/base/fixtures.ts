import { bindChannels } from '@optivem/optivem-testing';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { withApp } from '../../../../src/testkit/driver/adapter/shared/client/playwright/withApp.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

// Marks a scenario that logs in as a Keycloak user (customer, admin or anonymous): pass it as the test's
// details, e.g. test('name', requiresKeycloak, async ({ scenario }) => ...). Such scenarios are skipped
// unless a Keycloak URL is configured (KEYCLOAK_URL_STUB / KEYCLOAK_URL).
const REQUIRES_KEYCLOAK_TAG = '@requires-keycloak';
const requiresKeycloak = { tag: REQUIRES_KEYCLOAK_TAG } as const;

const keycloakConfigured = Boolean(loadConfiguration({ externalSystemMode: 'stub' }).keycloakUrl);

const _test = withApp('stub').extend<{ keycloakRequirement: void }>({
    keycloakRequirement: [
        async ({}, use, testInfo) => {
            testInfo.skip(
                testInfo.tags.includes(REQUIRES_KEYCLOAK_TAG) && !keycloakConfigured,
                'Identity scenarios require KEYCLOAK_URL to be set',
            );
            await use();
        },
        { auto: true },
    ],
});
const test = Object.assign(_test, {
    each: bindTestEach(_test),
    eachAlsoFirstRow: bindTestEach(_test, [ChannelType.API], [ChannelType.UI]),
});
const { forChannels } = bindChannels(test);
export { test, forChannels, requiresKeycloak };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
