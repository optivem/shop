import { test as base } from '@playwright/test';
import { ChannelContext, bindChannels } from '@optivem/optivem-testing';
import { bindTestEach } from '../../../../src/testkit/driver/adapter/shared/client/playwright/bindTestEach.js';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import type { MyShopDriver } from '../../../../src/testkit/driver/port/my-shop-driver.js';
import type { ErpDriver } from '../../../../src/testkit/driver/port/external/erp/erp-driver.js';
import type { TaxDriver } from '../../../../src/testkit/driver/port/external/tax/tax-driver.js';
import { MyShopApiDriver } from '../../../../src/testkit/driver/adapter/api/my-shop-api-driver.js';
import { MyShopUiDriver } from '../../../../src/testkit/driver/adapter/ui/my-shop-ui-driver.js';
import { ErpRealDriver } from '../../../../src/testkit/driver/adapter/external/erp/erp-real-driver.js';
import { TaxRealDriver } from '../../../../src/testkit/driver/adapter/external/tax/tax-real-driver.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
import { envOrDefault, nonEmptyOr } from '../../../../src/testkit/common/fallback.js';

process.env.EXTERNAL_SYSTEM_MODE = envOrDefault('EXTERNAL_SYSTEM_MODE', 'real');

const config = loadConfiguration();

const _test = base.extend<{ myShopDriver: MyShopDriver; erpDriver: ErpDriver; taxDriver: TaxDriver; _myShopBrowser: Browser }>({
    _myShopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    myShopDriver: async ({ _myShopBrowser }, use) => {
        const channel = nonEmptyOr(ChannelContext.get(), ChannelType.API);
        let driver: MyShopDriver;
        if (channel === ChannelType.UI) {
            driver = new MyShopUiDriver(config.myShop.frontendUrl, _myShopBrowser);
        } else {
            driver = new MyShopApiDriver(config.myShop.backendApiUrl);
        }
        await use(driver);
        await driver.close();
    },
    erpDriver: async ({}, use) => {
        const driver = new ErpRealDriver(config.externalSystems.erp.url);
        await use(driver);
        await driver.close();
    },
    taxDriver: async ({}, use) => {
        const driver = new TaxRealDriver(config.externalSystems.tax.url);
        await use(driver);
        await driver.close();
    },
});

const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
