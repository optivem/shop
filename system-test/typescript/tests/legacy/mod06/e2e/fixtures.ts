import { test as base } from '@playwright/test';
import { ChannelContext, bindChannels, bindTestEach } from '@optivem/optivem-testing';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import type { ShopDriver } from '../../../../src/testkit/driver/port/shop/shop-driver.js';
import type { ErpDriver } from '../../../../src/testkit/driver/port/external/erp/erp-driver.js';
import type { TaxDriver } from '../../../../src/testkit/driver/port/external/tax/tax-driver.js';
import { ShopApiDriver } from '../../../../src/testkit/driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from '../../../../src/testkit/driver/adapter/shop/ui/shop-ui-driver.js';
import { ErpStubDriver } from '../../../../src/testkit/driver/adapter/external/erp/erp-stub-driver.js';
import { TaxStubDriver } from '../../../../src/testkit/driver/adapter/external/tax/tax-stub-driver.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

// Channel-aware driver fixture: shopDriver switches between API/UI based on ChannelContext
const _test = base.extend<{ shopDriver: ShopDriver; erpDriver: ErpDriver; taxDriver: TaxDriver; _shopBrowser: Browser }>({
    _shopBrowser: async ({}, use) => {
        const browser = await chromium.launch();
        await use(browser);
        await browser.close();
    },
    shopDriver: async ({ _shopBrowser }, use) => {
        const channel = ChannelContext.get() || ChannelType.API;
        let driver: ShopDriver;
        if (channel === ChannelType.UI) {
            driver = new ShopUiDriver(config.shop.frontendUrl, _shopBrowser);
        } else {
            driver = new ShopApiDriver(config.shop.backendApiUrl);
        }
        await use(driver);
        await driver.close();
    },
    erpDriver: async ({}, use) => {
        const driver = new ErpStubDriver(config.externalSystems.erp.url);
        await use(driver);
        await driver.close();
    },
    taxDriver: async ({}, use) => {
        const driver = new TaxStubDriver(config.externalSystems.tax.url);
        await use(driver);
        await driver.close();
    },
});

const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
