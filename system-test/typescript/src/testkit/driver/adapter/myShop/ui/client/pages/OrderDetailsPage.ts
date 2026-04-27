import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

export class OrderDetailsPage extends BasePage {
  async waitForLoad(): Promise<void> {
    await this.page
      .locator("[aria-label='Display Order Number']")
      .waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
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
      (await this.page.locator("[aria-label='Display Order Number']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || ''
    );
  }

  async getOrderTimestamp(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Order Timestamp']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() ||
      ''
    );
  }

  async getSku(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display SKU']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || ''
    );
  }

  async getQuantity(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Quantity']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return parseInt(text, 10);
  }

  async getUnitPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Unit Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getBasePrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Base Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getDiscountRate(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Discount Rate']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('%', '')) / 100;
  }

  async getDiscountAmount(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Discount Amount']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getSubtotalPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Subtotal Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getTaxRate(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Tax Rate']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('%', '')) / 100;
  }

  async getTaxAmount(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Tax Amount']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getTotalPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Total Price']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '0';
    return Number.parseFloat(text.replace('$', ''));
  }

  async getCountry(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Country']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || ''
    );
  }

  async getAppliedCouponCode(): Promise<string | null> {
    const text = (await this.page.locator("[aria-label='Display Applied Coupon']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || '';
    return text === 'None' ? null : text;
  }

  async getStatus(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Status']").textContent({ timeout: PAGE_TIMEOUT_MS }))?.trim() || ''
    );
  }
}
