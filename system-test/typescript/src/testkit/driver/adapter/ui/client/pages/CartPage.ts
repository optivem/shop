import type { ManageCartLineDto, ManageCartResponse } from '../../../../port/dtos/ManageCartResponse.js';
import type { CartLineDto } from '../../../../port/dtos/ViewProductResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

const CART_LINES_SELECTOR = "table[aria-label='Cart Lines']";
const EMPTY_CART_SELECTOR = "p.lead:has-text('Your cart is empty.')";
const UNAVAILABLE_SELECTOR = 'main .alert-danger';
/** Exactly one of these shows once the cart has loaded: its lines, the empty cart or the ERP outage. */
export const CART_READY_SELECTOR = `${CART_LINES_SELECTOR}, ${EMPTY_CART_SELECTOR}, ${UNAVAILABLE_SELECTOR}`;
const CART_LINE_SELECTOR = `${CART_LINES_SELECTOR} tr[aria-label^='Cart Line ']`;
const CART_LINE_LABEL_PREFIX = 'Cart Line ';
const LINE_COUNT_SELECTOR = "[aria-label='Cart Line Count']";
const SUBTOTAL_SELECTOR = "[aria-label='Cart Subtotal']";
const CHECKOUT_NOTE_SELECTOR = "p:has-text('Tax and discounts are calculated at checkout')";
const START_SHOPPING_SELECTOR = "a:text-is('Start shopping')";
/** A typed quantity is committed when the field loses focus. */
const COMMIT_KEY = 'Tab';

const quantitySelector = (sku: string) => `input[aria-label='Quantity for ${sku}']`;
const titleSelector = (sku: string) => `[aria-label='Title ${sku}']`;
const unitPriceSelector = (sku: string) => `[aria-label='Unit Price ${sku}']`;
const lineTotalSelector = (sku: string) => `[aria-label='Line Total ${sku}']`;
const increaseSelector = (sku: string) => `button[aria-label='Increase Quantity for ${sku}']`;
const decreaseSelector = (sku: string) => `button[aria-label='Decrease Quantity for ${sku}']`;
const removeSelector = (sku: string) => `button[aria-label='Remove ${sku}']`;

/** The parts of a DOM form control the form check uses (the test project has no DOM typings). */
interface FormControl {
  form: unknown;
}

/**
 * The cart `/cart` (UC-008): its lines, quantity changes, removal and the subtotal. The server-rendered UIs post a
 * form and load the cart again for every change; the single-page UIs change it in place.
 */
export class CartPage extends BasePage {
  async waitForLoaded(): Promise<void> {
    await this.page.locator(CART_READY_SELECTOR).first().waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  async inputQuantity(sku: string, quantity: string): Promise<void> {
    const selector = quantitySelector(sku);
    await this.page.locator(selector).fill(quantity, { timeout: PAGE_TIMEOUT_MS });
    await this.change(selector, () => this.page.locator(selector).press(COMMIT_KEY, { timeout: PAGE_TIMEOUT_MS }));
  }

  async clickIncreaseQuantity(sku: string): Promise<void> {
    await this.click(increaseSelector(sku));
  }

  async clickDecreaseQuantity(sku: string): Promise<void> {
    await this.click(decreaseSelector(sku));
  }

  async clickRemove(sku: string): Promise<void> {
    await this.click(removeSelector(sku));
  }

  async readLines(): Promise<CartLineDto[]> {
    await this.waitForLoaded();
    const lines: CartLineDto[] = [];
    for (const sku of await this.readCartLineSkus()) {
      lines.push({ sku, quantity: await this.readQuantity(sku) });
    }
    return lines;
  }

  /** The cart as shown; `message` is the action's notification, if it had one. */
  async read(message: string | null, cartItemCount: number | null): Promise<ManageCartResponse> {
    await this.waitForLoaded();
    const lines: ManageCartLineDto[] = [];
    for (const sku of await this.readCartLineSkus()) {
      lines.push({
        sku,
        title: await this.text(titleSelector(sku)),
        unitPrice: await this.text(unitPriceSelector(sku)),
        quantity: await this.readQuantity(sku),
        lineTotal: await this.text(lineTotalSelector(sku)),
      });
    }
    return {
      message: message ?? (await this.readMessage()),
      cartItemCount: cartItemCount ?? (await this.readCartItemCount()),
      lines,
      itemCount: await this.readItemCount(),
      subtotal: (await this.isShown(SUBTOTAL_SELECTOR)) ? await this.text(SUBTOTAL_SELECTOR) : null,
      checkoutNote: (await this.isShown(CHECKOUT_NOTE_SELECTOR)) ? await this.text(CHECKOUT_NOTE_SELECTOR) : null,
      startShoppingOffered: await this.isShown(START_SHOPPING_SELECTOR),
    };
  }

  /** The notification an action on the cart showed, if any. */
  async readNotification(): Promise<string | null> {
    return this.readShownNotification();
  }

  private async click(selector: string): Promise<void> {
    await this.change(selector, () => this.page.locator(selector).click({ timeout: PAGE_TIMEOUT_MS }));
  }

  /** Runs a change and waits for the cart to show it: a new document when the control posts a form. */
  private async change(selector: string, action: () => Promise<void>): Promise<void> {
    if (await this.belongsToForm(selector)) {
      await this.runAndWaitForNewDocument(action);
    } else {
      await action();
    }
    await this.waitForLoaded();
  }

  /** Whether the element belongs to a form, so acting on it posts the form and loads a new document. */
  private async belongsToForm(selector: string): Promise<boolean> {
    return this.page
      .locator(selector)
      .evaluate((element: FormControl): boolean => element.form != null, undefined, { timeout: PAGE_TIMEOUT_MS });
  }

  /** Runs an action that loads a new document (such as a form post), and waits until the new document is ready. */
  private async runAndWaitForNewDocument(action: () => Promise<void>): Promise<void> {
    await this.page.evaluate('window.__previousDocument = true');
    await action();
    await this.page.waitForFunction("window.__previousDocument === undefined && document.readyState !== 'loading'", undefined, {
      timeout: PAGE_TIMEOUT_MS,
    });
  }

  private async readCartLineSkus(): Promise<string[]> {
    const skus: string[] = [];
    for (const row of await this.page.locator(CART_LINE_SELECTOR).all()) {
      skus.push(((await row.getAttribute('aria-label')) ?? '').substring(CART_LINE_LABEL_PREFIX.length));
    }
    return skus;
  }

  private async readQuantity(sku: string): Promise<number> {
    const quantity = await this.page.locator(quantitySelector(sku)).inputValue({ timeout: PAGE_TIMEOUT_MS });
    return Number.parseInt(quantity.trim(), 10);
  }

  /** "(2 items)" next to the heading. */
  private async readItemCount(): Promise<number | null> {
    if (!(await this.isShown(LINE_COUNT_SELECTOR))) return null;
    const digits = (await this.text(LINE_COUNT_SELECTOR)).replace(/\D+/g, ' ').trim().split(' ')[0] ?? '';
    return Number.parseInt(digits, 10);
  }

  private async readMessage(): Promise<string | null> {
    const notification = await this.readShownNotification();
    if (notification !== null) return notification;
    if (await this.isShown(EMPTY_CART_SELECTOR)) return this.text(EMPTY_CART_SELECTOR);
    if (await this.isShown(UNAVAILABLE_SELECTOR)) return this.text(UNAVAILABLE_SELECTOR);
    return null;
  }

  private async isShown(selector: string): Promise<boolean> {
    return this.page.locator(selector).first().isVisible();
  }

  private async text(selector: string): Promise<string> {
    return ((await this.page.locator(selector).first().textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }
}
