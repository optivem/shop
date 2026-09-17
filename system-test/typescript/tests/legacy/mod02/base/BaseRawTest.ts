import { randomUUID } from 'node:crypto';
import { chromium, type Browser, type BrowserContext, type Page } from '@playwright/test';
import { loadConfiguration, type TestConfig } from '../../../../config/configuration-loader.js';

export interface MyShopBrowser {
  browser: Browser;
  context: BrowserContext;
  page: Page;
}

export function getConfiguration(): TestConfig {
  return loadConfiguration();
}

export function getMyShopApiBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.myShop.backendApiUrl;
}

export function getMyShopUiBaseUrl(config: TestConfig = getConfiguration()): string {
  return config.myShop.frontendUrl;
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

export async function setUpMyShopBrowser(): Promise<MyShopBrowser> {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  return { browser, context, page };
}

export async function tearDownMyShopBrowser(myShopBrowser: MyShopBrowser | null): Promise<void> {
  if (!myShopBrowser) return;
  await myShopBrowser.page.close();
  await myShopBrowser.context.close();
  await myShopBrowser.browser.close();
}
