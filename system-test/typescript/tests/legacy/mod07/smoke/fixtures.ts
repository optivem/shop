import { test as base } from '@playwright/test';
import { ChannelContext, bindChannels, bindTestEach } from '@optivem/optivem-testing';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { ShopApiDriver } from '../../../../src/testkit/driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from '../../../../src/testkit/driver/adapter/shop/ui/shop-ui-driver.js';
import { ErpStubDriver } from '../../../../src/testkit/driver/adapter/external/erp/erp-stub-driver.js';
import { TaxStubDriver } from '../../../../src/testkit/driver/adapter/external/tax/tax-stub-driver.js';
import { ClockStubDriver } from '../../../../src/testkit/driver/adapter/external/clock/clock-stub-driver.js';
import { AppContext, UseCaseDsl } from '../../../../src/testkit/dsl/scenario-dsl.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

const _test = base.extend<{ useCase: UseCaseDsl; _shopBrowser: Browser }>({
    _shopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    useCase: async ({ _shopBrowser }, use) => {
        const channel = ChannelContext.get() || ChannelType.API;
        const app = new AppContext({
            channelMode: 'dynamic',
            channel,
            shopDriverFactory: (ch) => {
                if (ch === ChannelType.UI) {
                    return new ShopUiDriver(config.shop.frontendUrl, _shopBrowser);
                }
                return new ShopApiDriver(config.shop.backendApiUrl);
            },
            erpDriver: new ErpStubDriver(config.externalSystems.erp.url),
            clockDriver: new ClockStubDriver(config.externalSystems.clock.url),
            taxDriver: new TaxStubDriver(config.externalSystems.tax.url),
        });
        const useCase = new UseCaseDsl(app);
        await use(useCase);
        await useCase.close();
    },
});

const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
