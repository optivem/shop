import { test as base } from '@playwright/test';
import { ChannelContext, bindChannels } from '@optivem/optivem-testing';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import { MyShopApiDriver } from '../../../../src/testkit/driver/adapter/api/my-shop-api-driver.js';
import { MyShopUiDriver } from '../../../../src/testkit/driver/adapter/ui/my-shop-ui-driver.js';
import { ErpRealDriver } from '../../../../src/testkit/driver/adapter/external/erp/erp-real-driver.js';
import { TaxRealDriver } from '../../../../src/testkit/driver/adapter/external/tax/tax-real-driver.js';
import { ClockRealDriver } from '../../../../src/testkit/driver/adapter/external/clock/clock-real-driver.js';
import { AppContext, UseCaseDsl } from '../../../../src/testkit/dsl/scenario-dsl.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
import { nonEmptyOr } from '../../../../src/testkit/common/fallback.js';

const config = loadConfiguration();

const _test = base.extend<{ app: UseCaseDsl; _myShopBrowser: Browser }>({
    _myShopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    app: async ({ _myShopBrowser }, use) => {
        const channel = nonEmptyOr(ChannelContext.get(), ChannelType.API);
        const appContext = new AppContext({
            channelMode: 'dynamic',
            channel,
            myShopDriverFactory: (ch) => {
                if (ch === ChannelType.UI) {
                    return new MyShopUiDriver(config.myShop.frontendUrl, _myShopBrowser);
                }
                return new MyShopApiDriver(config.myShop.backendApiUrl);
            },
            erpDriver: new ErpRealDriver(config.externalSystems.erp.url),
            clockDriver: new ClockRealDriver(),
            taxDriver: new TaxRealDriver(config.externalSystems.tax.url),
        });
        const app = new UseCaseDsl(appContext);
        await use(app);
        await app.close();
    },
});

const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
