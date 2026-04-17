import { test as base } from '@playwright/test';
import { ChannelContext, bindChannels, bindTestEach } from '@optivem/optivem-testing';
import { chromium } from 'playwright';
import type { Browser } from 'playwright';
import { loadConfiguration } from '../../../../config/configuration-loader.js';
import type { ShopDriver } from '../../../../src/testkit/driver/port/shop/shop-driver.js';
import { ShopApiDriver } from '../../../../src/testkit/driver/adapter/shop/api/shop-api-driver.js';
import { ShopUiDriver } from '../../../../src/testkit/driver/adapter/shop/ui/shop-ui-driver.js';
import { ChannelType } from '../../../../src/testkit/channel/channel-type.js';

process.env.EXTERNAL_SYSTEM_MODE = process.env.EXTERNAL_SYSTEM_MODE || 'stub';

const config = loadConfiguration();

const _test = base.extend<{ shopDriver: ShopDriver; _shopBrowser: Browser }>({
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
});

const test = Object.assign(_test, { each: bindTestEach(_test) });
const { forChannels } = bindChannels(test);
export { test, forChannels };
export { ChannelType } from '../../../../src/testkit/channel/channel-type.js';
export { expect } from '@playwright/test';
