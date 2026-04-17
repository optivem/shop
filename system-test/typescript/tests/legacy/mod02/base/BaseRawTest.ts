import { randomUUID } from 'node:crypto';
import { chromium, type Browser, type BrowserContext, type Page } from 'playwright';
import { loadConfiguration, type TestConfig } from '../../../../config/configuration-loader.js';

export interface ShopBrowser {
  browser: Browser;
  context: BrowserContext;
  page: Page;
}

export function getConfiguration(): TestConfig {
  return loadConfiguration();
}

export function getShopApiBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.shop.backendApiUrl;
}

export function getShopUiBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.shop.frontendUrl;
}

export function getErpBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.externalSystems.erp.url;
}

export function getTaxBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.externalSystems.tax.url;
}

export function createUniqueSku(baseSku: string): string {
  const suffix = randomUUID().substring(0, 8);
  return `${baseSku}-${suffix}`;
}

export async function setUpShopBrowser(): Promise<ShopBrowser> {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  return { browser, context, page };
}

export async function tearDownShopBrowser(shopBrowser: ShopBrowser | null): Promise<void> {
  if (!shopBrowser) return;
  await shopBrowser.page.close().catch(() => {});
  await shopBrowser.context.close().catch(() => {});
  await shopBrowser.browser.close().catch(() => {});
}
