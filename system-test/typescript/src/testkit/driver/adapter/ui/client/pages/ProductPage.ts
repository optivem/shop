import type { Locator } from '@playwright/test';
import type { ViewProductResponse } from '../../../../port/dtos/ViewProductResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';
import { CatalogPage } from './CatalogPage.js';

const TITLE_SELECTOR = "h1[aria-label='Product Title']";
const BACK_TO_CATALOG_SELECTOR = "[aria-label='Back to Catalog']";
const NOT_FOUND_HEADING_SELECTOR = `:has(> ${BACK_TO_CATALOG_SELECTOR}) > h1`;
const UNAVAILABLE_SELECTOR = 'main .alert-danger';
/** Exactly one of these shows once the product page has loaded: the product, product not found or the ERP outage. */
export const PRODUCT_READY_SELECTOR = `:is(${TITLE_SELECTOR}, ${BACK_TO_CATALOG_SELECTOR}, ${UNAVAILABLE_SELECTOR})`;
const BRAND_SELECTOR = "[aria-label='Product Brand']";
const CATEGORY_SELECTOR = "[aria-label='Product Category']";
const SKU_SELECTOR = "[aria-label='Product SKU']";
const PRICE_SELECTOR = "[aria-label='Display Price']";
const DESCRIPTION_SELECTOR = "[aria-label='Product Description']";
const BREADCRUMB_SELECTOR = "nav[aria-label='breadcrumb'] .breadcrumb-item";
const QUANTITY_SELECTOR = "input[aria-label='Product Quantity']";
const INCREASE_SELECTOR = "button[aria-label='Increase Quantity']";
const DECREASE_SELECTOR = "button[aria-label='Decrease Quantity']";
const ADD_TO_CART_SELECTOR = "button[aria-label='Add to Cart']";
const CART_ITEM_COUNT_SELECTOR = "[aria-label='Cart Item Count']";
const PRODUCT_PATH_PREFIX = '/products/';

/** A product's page `/products/{sku}` (UC-007): its details, the quantity stepper and "Add to cart". */
export class ProductPage extends BasePage {
  private notificationMessage: string | null = null;

  async waitForLoaded(): Promise<void> {
    await this.page.locator(PRODUCT_READY_SELECTOR).first().waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  async inputQuantity(quantity: string): Promise<void> {
    await this.page.locator(QUANTITY_SELECTOR).fill(quantity, { timeout: PAGE_TIMEOUT_MS });
  }

  async clickIncreaseQuantity(): Promise<void> {
    await this.page.locator(INCREASE_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
  }

  async clickDecreaseQuantity(): Promise<void> {
    await this.page.locator(DECREASE_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
  }

  /** Adds to the cart and waits for the confirmation or the error, which read() reports as the message. */
  async clickAddToCart(): Promise<void> {
    await this.page.locator(ADD_TO_CART_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
    const result = await this.getResult();
    this.notificationMessage = (result.success ? result.value : result.error.message).trim();
    await this.waitForLoaded();
  }

  async clickBackToCatalog(): Promise<CatalogPage> {
    const previousUrl = this.page.url();
    await this.page.locator(BACK_TO_CATALOG_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
    await this.page.waitForURL((url) => url.toString() !== previousUrl, { timeout: PAGE_TIMEOUT_MS });
    const catalogPage = new CatalogPage(this.page);
    await catalogPage.waitForLoaded();
    return catalogPage;
  }

  async read(): Promise<ViewProductResponse> {
    await this.waitForLoaded();
    const response: ViewProductResponse = {
      message: await this.readMessage(),
      backToCatalogOffered: await this.isShown(BACK_TO_CATALOG_SELECTOR),
      addToCartOffered: await this.isShown(ADD_TO_CART_SELECTOR),
      cartItemCount: await this.readCartItemCount(),
      pageSku: this.readPageSku(),
      breadcrumbs: [],
    };
    if (!(await this.isShown(TITLE_SELECTOR))) {
      return response;
    }
    return {
      ...response,
      title: await this.text(this.page.locator(TITLE_SELECTOR)),
      brand: await this.text(this.page.locator(BRAND_SELECTOR)),
      category: await this.text(this.page.locator(CATEGORY_SELECTOR)),
      sku: await this.text(this.page.locator(SKU_SELECTOR)),
      price: await this.text(this.page.locator(PRICE_SELECTOR)),
      description: await this.text(this.page.locator(DESCRIPTION_SELECTOR)),
      breadcrumbs: (await this.page.locator(BREADCRUMB_SELECTOR).allTextContents()).map((item) => item.trim()),
      quantity: await this.page.locator(QUANTITY_SELECTOR).inputValue({ timeout: PAGE_TIMEOUT_MS }),
    };
  }

  private async readMessage(): Promise<string | null> {
    if (this.notificationMessage !== null) return this.notificationMessage;
    if (await this.isShown(NOT_FOUND_HEADING_SELECTOR)) return this.text(this.page.locator(NOT_FOUND_HEADING_SELECTOR));
    if (await this.isShown(UNAVAILABLE_SELECTOR)) return this.text(this.page.locator(UNAVAILABLE_SELECTOR));
    return null;
  }

  private async readCartItemCount(): Promise<number> {
    if (!(await this.isShown(CART_ITEM_COUNT_SELECTOR))) return 0;
    return Number.parseInt(await this.text(this.page.locator(CART_ITEM_COUNT_SELECTOR)), 10);
  }

  private readPageSku(): string | null {
    const path = new URL(this.page.url()).pathname;
    return path.startsWith(PRODUCT_PATH_PREFIX) ? decodeURIComponent(path.substring(PRODUCT_PATH_PREFIX.length)) : null;
  }

  private async isShown(selector: string): Promise<boolean> {
    return (await this.page.locator(selector).count()) > 0;
  }

  private async text(locator: Locator): Promise<string> {
    return ((await locator.textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }
}
