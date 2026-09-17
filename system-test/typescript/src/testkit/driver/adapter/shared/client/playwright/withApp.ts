// TypeScript-specific Playwright fixture that provides a `scenario` per test.
// Java/.NET use abstract base test classes for the equivalent setup; TS uses
// `test.extend(...)` composition, so spec fixtures import and invoke this.
import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import { ChannelType } from '../../../../../channel/channel-type.js';
import { createScenario, type Channel, type ExternalSystemMode } from '../../../../../test-setup.js';
import type { ScenarioDsl } from '../../../../../dsl/scenario-dsl.js';
import { envOrDefault } from '../../../../../common/fallback.js';

export function withApp() {
    return base.extend<{ scenario: ScenarioDsl }>({
        // eslint-disable-next-line no-empty-pattern -- Playwright requires an object pattern for fixtures with no dependencies
        scenario: async ({}, use) => {
            const channel = envOrDefault('CHANNEL', ChannelType.API) as Channel;
            const mode = envOrDefault('EXTERNAL_SYSTEM_MODE', 'real').toLowerCase() as ExternalSystemMode;
            let browser;
            if (channel === ChannelType.UI) {
                browser = await chromium.launch();
            }
            const scenario = createScenario({ channel, externalSystemMode: mode, browser });
            await use(scenario);
            await scenario.close();
            if (browser) await browser.close();
        },
    });
}
