// TypeScript-specific Playwright fixture that provides a `scenario` per test.
// Java/.NET use abstract base test classes for the equivalent setup; TS uses
// `test.extend(...)` composition, so spec fixtures import and invoke this.
//
// `externalSystemMode` is a Playwright option: it defaults to EXTERNAL_SYSTEM_MODE
// (or `defaultExternalSystemMode` when the env var is unset), and a spec tied to
// one mode pins it with `test.use({ externalSystemMode: 'stub' })`.
import { test as base, type Browser } from '@playwright/test';
import { ChannelType } from '../../../../../channel/channel-type.js';
import { channelFromEnv, externalSystemModeFromEnv } from '../../../../../common/env.js';
import { createScenario, type ExternalSystemMode } from '../../../../../test-setup.js';
import type { ScenarioDsl } from '../../../../../dsl/scenario-dsl.js';

export interface AppOptions {
    externalSystemMode: ExternalSystemMode;
}

export function withApp(defaultExternalSystemMode: ExternalSystemMode = 'real') {
    const channel = channelFromEnv();

    async function useScenario(externalSystemMode: ExternalSystemMode, browser: Browser | undefined, use: (scenario: ScenarioDsl) => Promise<void>) {
        const scenario = createScenario({ channel, externalSystemMode, browser });
        try {
            await use(scenario);
        } finally {
            await scenario.close();
        }
    }

    return base.extend<AppOptions & { scenario: ScenarioDsl }>({
        externalSystemMode: [externalSystemModeFromEnv(defaultExternalSystemMode), { option: true }],
        // Only the UI channel depends on Playwright's worker-scoped `browser` fixture,
        // so API runs never launch Chromium.
        scenario:
            channel === ChannelType.UI
                ? async ({ externalSystemMode, browser }, use) => useScenario(externalSystemMode, browser, use)
                : async ({ externalSystemMode }, use) => useScenario(externalSystemMode, undefined, use),
    });
}
