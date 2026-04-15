import { loadConfiguration, TestConfig } from '../../config/configuration-loader.js';
import { ScenarioDsl, AppContext } from './dsl/scenario-dsl.js';
import type { ChannelMode } from './dsl/scenario-dsl.js';
import { UseCaseContext } from './dsl/core/use-case-context.js';
import { ShopApiDriver } from './driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from './driver/adapter/shop/ui/shop-ui-driver.js';
import { ErpRealDriver } from './driver/adapter/external/erp/erp-real-driver.js';
import { ErpStubDriver } from './driver/adapter/external/erp/erp-stub-driver.js';
import { ClockRealDriver } from './driver/adapter/external/clock/clock-real-driver.js';
import { ClockStubDriver } from './driver/adapter/external/clock/clock-stub-driver.js';
import { TaxRealDriver } from './driver/adapter/external/tax/tax-real-driver.js';
import { TaxStubDriver } from './driver/adapter/external/tax/tax-stub-driver.js';
import { ErpDriver } from './driver/port/external/erp/erp-driver.js';
import { ClockDriver } from './driver/port/external/clock/clock-driver.js';
import { TaxDriver } from './driver/port/external/tax/tax-driver.js';
import { Browser } from 'playwright';

export type Channel = 'api' | 'ui';
export type { ChannelMode } from './dsl/scenario-dsl.js';
export type ExternalSystemMode = 'real' | 'stub';

export interface ScenarioOptions {
  channel?: Channel;
  channelMode?: ChannelMode;
  externalSystemMode?: ExternalSystemMode;
  browser?: Browser;
}

export function createScenario(options: ScenarioOptions = {}): ScenarioDsl {
  const mode = options.externalSystemMode || 'real';
  const config = loadConfiguration({ externalSystemMode: mode });

  const channelMode: ChannelMode = options.channelMode || (process.env.CHANNEL_MODE?.toLowerCase() as ChannelMode) || 'dynamic';
  const channel = options.channel || 'api';

  const app = new AppContext({
    channelMode,
    channel,
    shopDriverFactory: (ch) => createShopDriverForChannel(config, ch as Channel, options),
    erpDriver: createErpDriver(config, mode),
    clockDriver: createClockDriver(config, mode),
    taxDriver: createTaxDriver(config, mode),
  });

  const useCaseContext = new UseCaseContext(mode);
  return new ScenarioDsl(app, useCaseContext);
}

function createShopDriverForChannel(config: TestConfig, channel: Channel, options: ScenarioOptions) {
  if (channel === 'ui') {
    if (!options.browser) throw new Error('Browser is required for UI channel');
    return new ShopUiDriver(config.shop.frontendUrl, options.browser);
  }
  return new ShopApiDriver(config.shop.backendApiUrl);
}

function createErpDriver(config: TestConfig, mode: ExternalSystemMode): ErpDriver {
  if (mode === 'stub') return new ErpStubDriver(config.externalSystems.erp.url);
  return new ErpRealDriver(config.externalSystems.erp.url);
}

function createClockDriver(config: TestConfig, mode: ExternalSystemMode): ClockDriver {
  if (mode === 'stub') return new ClockStubDriver(config.externalSystems.clock.url);
  return new ClockRealDriver();
}

function createTaxDriver(config: TestConfig, mode: ExternalSystemMode): TaxDriver {
  if (mode === 'stub') return new TaxStubDriver(config.externalSystems.tax.url);
  return new TaxRealDriver(config.externalSystems.tax.url);
}
