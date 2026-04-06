import { loadConfiguration, TestConfig } from '../config/configuration-loader';
import { ScenarioDsl, AppContext } from './dsl/scenario-dsl';
import { ShopApiDriver } from './drivers/shop-api-driver';
import { ShopUiDriver } from './drivers/shop-ui-driver';
import { ErpRealDriver } from './drivers/erp-real-driver';
import { ErpStubDriver } from './drivers/erp-stub-driver';
import { ClockRealDriver } from './drivers/clock-real-driver';
import { ClockStubDriver } from './drivers/clock-stub-driver';
import { ShopDriver, ErpDriver, ClockDriver } from './drivers/types';
import { Browser } from 'playwright';

export type Channel = 'api' | 'ui';
export type ChannelMode = 'dynamic' | 'static';
export type ExternalSystemMode = 'real' | 'stub';

const STATIC_CHANNEL: Channel = 'api';

export interface ScenarioOptions {
  channel?: Channel;
  channelMode?: ChannelMode;
  externalSystemMode?: ExternalSystemMode;
  browser?: Browser;
}

export function createScenario(options: ScenarioOptions = {}): ScenarioDsl {
  const mode = options.externalSystemMode || 'real';
  const config = loadConfiguration({ externalSystemMode: mode });

  const channelMode = options.channelMode || (process.env.CHANNEL_MODE?.toLowerCase() as ChannelMode) || 'dynamic';

  const actionShopDriver = createShopDriverForChannel(config, options.channel || 'api', options);

  let shopDriver: ShopDriver;
  if (channelMode === 'static') {
    shopDriver = createShopDriverForChannel(config, STATIC_CHANNEL, options);
  } else {
    shopDriver = actionShopDriver;
  }

  const erpDriver = createErpDriver(config, mode);
  const clockDriver = createClockDriver(config, mode);

  const app: AppContext = { shopDriver, actionShopDriver, erpDriver, clockDriver };
  return new ScenarioDsl(app);
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
