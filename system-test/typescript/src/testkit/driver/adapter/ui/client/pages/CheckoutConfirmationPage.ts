import type { CheckOutConfirmedOrderDto, CheckOutRejectedLineDto, CheckOutResponse } from '../../../../port/dtos/CheckOutResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

const CONFIRMED_ORDERS_SELECTOR = "ul[aria-label='Confirmed Orders']";
const REJECTED_LINES_SELECTOR = "[aria-label='Rejected Lines']";
const NO_RECENT_ORDER_SELECTOR = "h1:has-text('No recent order')";
/** Exactly one of the confirmed orders or "No recent order" shows once the confirmation has loaded. */
export const CHECKOUT_CONFIRMATION_READY_SELECTOR = `${CONFIRMED_ORDERS_SELECTOR}, ${NO_RECENT_ORDER_SELECTOR}`;
const CONFIRMED_ORDER_SELECTOR = `${CONFIRMED_ORDERS_SELECTOR} a[aria-label^='Confirmed Order ']`;
const REJECTED_LINE_SELECTOR = `${REJECTED_LINES_SELECTOR} li[aria-label^='Rejected Line ']`;
const REJECTED_LINE_LABEL_PREFIX = 'Rejected Line ';
const THANKS_SELECTOR = "h1:has-text('Thank you')";
const VIEW_MY_ORDERS_SELECTOR = "a:text-is('View my orders')";
const CONTINUE_SHOPPING_SELECTOR = "a:text-is('Continue shopping')";

const rejectionReasonSelector = (sku: string) => `[aria-label='Rejection Reason ${sku}']`;

/** The order confirmation `/checkout/confirmation` (UC-009): the orders just placed and the rejected lines. */
export class CheckoutConfirmationPage extends BasePage {
  async isShown(): Promise<boolean> {
    return this.isVisible(CHECKOUT_CONFIRMATION_READY_SELECTOR);
  }

  async readInto(response: CheckOutResponse): Promise<void> {
    await this.page.locator(CHECKOUT_CONFIRMATION_READY_SELECTOR).first().waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    response.thanks = (await this.isVisible(THANKS_SELECTOR)) ? await this.text(THANKS_SELECTOR) : null;
    const orders: CheckOutConfirmedOrderDto[] = [];
    for (const link of await this.page.locator(CONFIRMED_ORDER_SELECTOR).all()) {
      orders.push({ orderNumber: ((await link.textContent()) ?? '').trim(), link: await link.getAttribute('href') });
    }
    response.confirmedOrders = orders;
    const rejectedLines: CheckOutRejectedLineDto[] = [];
    for (const line of await this.page.locator(REJECTED_LINE_SELECTOR).all()) {
      const sku = ((await line.getAttribute('aria-label')) ?? '').substring(REJECTED_LINE_LABEL_PREFIX.length);
      rejectedLines.push({ sku, reason: await this.text(rejectionReasonSelector(sku)) });
    }
    response.rejectedLines = rejectedLines;
    response.viewMyOrdersOffered = await this.isVisible(VIEW_MY_ORDERS_SELECTOR);
    response.continueShoppingOffered = await this.isVisible(CONTINUE_SHOPPING_SELECTOR);
  }

  private async isVisible(selector: string): Promise<boolean> {
    return this.page.locator(selector).first().isVisible();
  }

  private async text(selector: string): Promise<string> {
    return ((await this.page.locator(selector).first().textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }
}
