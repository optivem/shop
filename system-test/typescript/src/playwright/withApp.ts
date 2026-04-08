import { test as base } from '@playwright/test';
import { chromium } from 'playwright';
import { createScenario, type Channel, type ExternalSystemMode } from '../test-setup.js';
import type { ScenarioDsl } from '../dsl/scenario/scenario-dsl.js';

export function withApp() {
    return base.extend<{ scenario: ScenarioDsl }>({
        scenario: async ({}, use) => {
            const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
            const mode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'real') as ExternalSystemMode;
            let browser;
            if (channel === 'ui') {
                browser = await chromium.launch();
            }
            const scenario = createScenario({ channel, externalSystemMode: mode, browser });
            await use(scenario);
            await scenario.close();
            if (browser) await browser.close();
        },
    });
}
