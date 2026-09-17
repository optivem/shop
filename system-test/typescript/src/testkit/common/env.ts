// Test-run settings read from env vars (CHANNEL, CHANNEL_MODE, EXTERNAL_SYSTEM_MODE). Each is parsed
// against its allowed values, so a typo fails fast with a clear message instead of being cast into the
// DSL and silently selecting the wrong driver.

import { ChannelType, type ChannelTypeValue } from '../channel/channel-type.js';
import type { ChannelMode } from '../dsl/port/channel-mode.js';
import type { ExternalSystemMode } from '../dsl/port/external-system-mode.js';
import { envOrDefault } from './fallback.js';

function envChoice<T extends string>(name: string, allowed: readonly T[], defaultValue: T, normalize: (value: string) => string): T {
    const value = normalize(envOrDefault(name, defaultValue));
    const match = allowed.find((candidate) => candidate === value);
    if (match === undefined) {
        throw new Error(`Invalid ${name}='${process.env[name]}' — expected one of: ${allowed.join(', ')}`);
    }
    return match;
}

export function channelFromEnv(): ChannelTypeValue {
    return envChoice('CHANNEL', [ChannelType.API, ChannelType.UI], ChannelType.API, (value) => value.toUpperCase());
}

export function channelModeFromEnv(): ChannelMode {
    return envChoice<ChannelMode>('CHANNEL_MODE', ['dynamic', 'static'], 'dynamic', (value) => value.toLowerCase());
}

export function externalSystemModeFromEnv(defaultValue: ExternalSystemMode = 'real'): ExternalSystemMode {
    return envChoice<ExternalSystemMode>('EXTERNAL_SYSTEM_MODE', ['real', 'stub'], defaultValue, (value) => value.toLowerCase());
}
