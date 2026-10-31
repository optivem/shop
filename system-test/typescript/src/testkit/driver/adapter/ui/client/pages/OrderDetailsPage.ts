import type { Result } from '../../../../../common/result.js';
import { success, failure } from '../../../../../common/result.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';
import { nonEmptyOr } from '../../../../../common/fallback.js';

const ORDER_LOADED_SELECTOR = "[aria-label='Display Order Number']:visible";
const ORDER_NOT_FOUND_SELECTOR = "p:has-text('Order not found'):visible";
const ORDER_ERROR_SELECTOR = ".alert-danger:visible, .notification.error:visible";
const ORDER_ERROR_PREFIX = 'Error:';

export class OrderDetailsPage extends BasePage {
  /** Waits for the page to show the order, or what it shows instead when the order can't be viewed. */
  async getLoadResult(orderNumber: string): Promise<Result<void, SystemError>> {
    await this.page
      .locator([ORDER_LOADED_SELECTOR, ORDER_NOT_FOUND_SELECTOR, ORDER_ERROR_SELECTOR].join(', '))
      .waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });

    if ((await this.page.locator(ORDER_LOADED_SELECTOR).count()) > 0) {
      return success(undefined);
    }

    if ((await this.page.locator(ORDER_NOT_FOUND_SELECTOR).count()) > 0) {
      return failure({ message: `Order ${orderNumber} does not exist.`, fieldErrors: [] });
    }

    const errorText = (await this.page.locator(ORDER_ERROR_SELECTOR).textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '';
    return failure({ message: errorText.replace(ORDER_ERROR_PREFIX, '').trim(), fieldErrors: [] });
  }

  async clickCancelOrder(): Promise<void> {
    const cancelButton = this.page.locator('[aria-label="Cancel Order"]');
    await cancelButton.waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    await cancelButton.click({ timeout: PAGE_TIMEOUT_MS });
  }

  async clickDeliverOrder(): Promise<void> {
    const deliverButton = this.page.locator('[aria-label="Deliver Order"]');
    await deliverButton.waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    await deliverButton.click({ timeout: PAGE_TIMEOUT_MS });
  }

  async getOrderNumber(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Order Number']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? ''
    );
  }

  async getOrderTimestamp(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Order Timestamp']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ??
      ''
    );
  }

  async getSku(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display SKU']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? ''
    );
  }

  async getQuantity(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Quantity']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseInt(text, 10);
  }

  async getUnitPrice(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Unit Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getBasePrice(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Base Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getDiscountRate(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Discount Rate']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('%', '')) / 100;
  }

  async getDiscountAmount(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Discount Amount']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getSubtotalPrice(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Subtotal Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getTaxRate(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Tax Rate']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('%', '')) / 100;
  }

  async getTaxAmount(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Tax Amount']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getTotalPrice(): Promise<number> {
    const text = nonEmptyOr(
      (await this.page.locator("[aria-label='Display Total Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim(),
      '0',
    );
    return Number.parseFloat(text.replace('$', ''));
  }

  async getCountry(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Country']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? ''
    );
  }

  async getAppliedCouponCode(): Promise<string | null> {
    const text = (await this.page.locator("[aria-label='Display Applied Coupon']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? '';
    return text === 'None' ? null : text;
  }

  async getStatus(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Status']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ?? ''
    );
  }
}
