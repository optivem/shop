import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

export class NewOrderPage extends BasePage {
  async inputSku(sku: string): Promise<void> {
    await this.page.locator('[aria-label="SKU"]').fill(sku, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputQuantity(quantity: string): Promise<void> {
    await this.page.locator('[aria-label="Quantity"]').fill(quantity, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputCountry(country: string): Promise<void> {
    await this.page.locator('[aria-label="Country"]').fill(country, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputCouponCode(couponCode: string): Promise<void> {
    await this.page.locator('[aria-label="Coupon Code"]').fill(couponCode, { timeout: PAGE_TIMEOUT_MS });
  }

  async clickPlaceOrder(): Promise<void> {
    await this.page.locator('[aria-label="Place Order"]').click({ timeout: PAGE_TIMEOUT_MS });
  }

  static getOrderNumber(successMessage: string): string | null {
    const match = successMessage.match(/Order has been created with Order Number ([\w-]+)/);
    return match ? match[1] : null;
  }
}
