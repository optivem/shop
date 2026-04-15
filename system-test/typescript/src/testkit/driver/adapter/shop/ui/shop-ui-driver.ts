import { Browser, BrowserContext, Page } from 'playwright';
import { Result, success, failure } from '../../../../common/result.js';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  PublishCouponRequest,
  BrowseCouponsResponse,
} from '../../../../common/dtos.js';
import { ShopDriver } from '../../../port/shop/shop-driver.js';

const TIMEOUT = 30_000;

// --- Page Objects ---

class HomePage {
  constructor(private page: Page) {}

  async clickNewOrder(): Promise<void> {
    await this.page.locator("a[href='/new-order']").click({ timeout: TIMEOUT });
  }

  async clickOrderHistory(): Promise<void> {
    await this.page.locator("a[href='/order-history']").click({ timeout: TIMEOUT });
  }

  async clickAdminCoupons(): Promise<void> {
    await this.page.locator("a[href='/admin-coupons']").click({ timeout: TIMEOUT });
  }
}

class NewOrderPage {
  constructor(private page: Page) {}

  async fillSku(sku: string): Promise<void> {
    await this.page.locator('[aria-label="SKU"]').fill(sku, { timeout: TIMEOUT });
  }

  async fillQuantity(quantity: string): Promise<void> {
    await this.page.locator('[aria-label="Quantity"]').fill(quantity, { timeout: TIMEOUT });
  }

  async fillCountry(country: string): Promise<void> {
    await this.page.locator('[aria-label="Country"]').fill(country, { timeout: TIMEOUT });
  }

  async fillCouponCode(couponCode: string): Promise<void> {
    await this.page.locator('[aria-label="Coupon Code"]').fill(couponCode, { timeout: TIMEOUT });
  }

  async clickPlaceOrder(): Promise<void> {
    await this.page.locator('[aria-label="Place Order"]').click({ timeout: TIMEOUT });
  }

  static getOrderNumber(successMessage: string): string | null {
    const match = successMessage.match(/Order has been created with Order Number ([\w-]+)/);
    return match ? match[1] : null;
  }
}

class OrderHistoryPage {
  constructor(private page: Page) {}

  async fillOrderNumber(orderNumber: string): Promise<void> {
    await this.page.locator("[aria-label='Order Number']").fill(orderNumber, { timeout: TIMEOUT });
  }

  async clickSearch(): Promise<void> {
    await this.page.locator("[aria-label='Refresh Order List']").click({ timeout: TIMEOUT });
  }

  async clickViewOrderDetails(orderNumber: string): Promise<void> {
    const row = this.page.locator(`//tr[contains(., '${orderNumber}')]`);
    await row.locator("//a[contains(text(), 'View Details')]").click({ timeout: TIMEOUT });
  }
}

class OrderDetailsPage {
  constructor(private page: Page) {}

  async waitForLoad(): Promise<void> {
    await this.page
      .locator("[aria-label='Display Order Number']")
      .waitFor({ state: 'visible', timeout: TIMEOUT });
  }

  async clickCancelOrder(): Promise<void> {
    const cancelButton = this.page.locator('[aria-label="Cancel Order"]');
    await cancelButton.waitFor({ state: 'visible', timeout: TIMEOUT });
    await cancelButton.click({ timeout: TIMEOUT });
  }

  async clickDeliverOrder(): Promise<void> {
    const deliverButton = this.page.locator('[aria-label="Deliver Order"]');
    await deliverButton.waitFor({ state: 'visible', timeout: TIMEOUT });
    await deliverButton.click({ timeout: TIMEOUT });
  }

  async getOrderNumber(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Order Number']").textContent({ timeout: TIMEOUT }))?.trim() || ''
    );
  }

  async getOrderTimestamp(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Order Timestamp']").textContent({ timeout: TIMEOUT }))?.trim() ||
      ''
    );
  }

  async getSku(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display SKU']").textContent({ timeout: TIMEOUT }))?.trim() || ''
    );
  }

  async getQuantity(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Quantity']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseInt(text, 10);
  }

  async getUnitPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Unit Price']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getBasePrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Base Price']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getDiscountRate(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Discount Rate']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('%', '')) / 100;
  }

  async getDiscountAmount(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Discount Amount']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getSubtotalPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Subtotal Price']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getTaxRate(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Tax Rate']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('%', '')) / 100;
  }

  async getTaxAmount(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Tax Amount']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getTotalPrice(): Promise<number> {
    const text =
      (await this.page.locator("[aria-label='Display Total Price']").textContent({ timeout: TIMEOUT }))?.trim() || '0';
    return parseFloat(text.replace('$', ''));
  }

  async getCountry(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Country']").textContent({ timeout: TIMEOUT }))?.trim() || ''
    );
  }

  async getAppliedCouponCode(): Promise<string | null> {
    const text = (await this.page.locator("[aria-label='Display Applied Coupon']").textContent({ timeout: TIMEOUT }))?.trim() || '';
    return text === 'None' ? null : text;
  }

  async getStatus(): Promise<string> {
    return (
      (await this.page.locator("[aria-label='Display Status']").textContent({ timeout: TIMEOUT }))?.trim() || ''
    );
  }
}

class AdminCouponsPage {
  constructor(private page: Page) {}

  async fillCouponCode(code: string): Promise<void> {
    await this.page.locator('[aria-label="Coupon Code"]').fill(code, { timeout: TIMEOUT });
  }

  async fillDiscountRate(rate: number): Promise<void> {
    await this.page.locator('[aria-label="Discount Rate"]').fill(rate.toString(), { timeout: TIMEOUT });
  }

  async fillValidFrom(dateStr: string): Promise<void> {
    const localValue = dateStr.replace('Z', '').replace('T', 'T').slice(0, 16);
    await this.page.locator('[aria-label="Valid From"]').fill(localValue, { timeout: TIMEOUT });
  }

  async fillValidTo(dateStr: string): Promise<void> {
    const localValue = dateStr.replace('Z', '').replace('T', 'T').slice(0, 16);
    await this.page.locator('[aria-label="Valid To"]').fill(localValue, { timeout: TIMEOUT });
  }

  async fillUsageLimit(limit: number): Promise<void> {
    await this.page.locator('[aria-label="Usage Limit"]').fill(limit.toString(), { timeout: TIMEOUT });
  }

  async clickCreateCoupon(): Promise<void> {
    await this.page.locator('[aria-label="Create Coupon"]').click({ timeout: TIMEOUT });
  }

  async clickRefreshCouponList(): Promise<void> {
    await this.page.locator('[aria-label="Refresh Coupon List"]').click({ timeout: TIMEOUT });
  }

  async getCouponRows(): Promise<Array<{ code: string; discountRate: number; validFrom?: string; validTo?: string; usageLimit?: number; usedCount: number }>> {
    const table = this.page.locator("[aria-label='Coupons Table']");
    await table.waitFor({ state: 'visible', timeout: TIMEOUT });
    const rows = table.locator('tbody tr');
    const count = await rows.count();
    const result: Array<{ code: string; discountRate: number; validFrom?: string; validTo?: string; usageLimit?: number; usedCount: number }> = [];
    for (let i = 0; i < count; i++) {
      const cells = rows.nth(i).locator('td');
      const code = (await cells.nth(0).textContent())?.trim() || '';
      const rateText = (await cells.nth(1).textContent())?.trim() || '0';
      const discountRate = Number.parseFloat(rateText.replace('%', '')) / 100;
      const validFrom = parseDisplayedDateToIso((await cells.nth(2).textContent()) ?? undefined);
      const validTo = parseDisplayedDateToIso((await cells.nth(3).textContent()) ?? undefined);
      const usageLimitText = (await cells.nth(4).textContent())?.trim() ?? '';
      const usageLimit = usageLimitText === 'Unlimited' || usageLimitText === '' ? undefined : Number.parseInt(usageLimitText, 10);
      const usedCountText = (await cells.nth(5).textContent())?.trim() ?? '0';
      const usedCount = Number.parseInt(usedCountText, 10) || 0;
      result.push({ code, discountRate, validFrom, validTo, usageLimit, usedCount });
    }
    return result;
  }
}

// --- Date Parsing ---

function parseDisplayedDateToIso(displayed: string | undefined): string | undefined {
  if (!displayed || displayed.trim() === '') return undefined;
  const parsed = new Date(displayed.trim());
  if (isNaN(parsed.getTime())) return displayed.trim();
  // The frontend displays UTC values in locale format. Parse the components
  // and reconstruct as UTC to avoid local-timezone shifts.
  const utcMs = Date.UTC(
    parsed.getFullYear(), parsed.getMonth(), parsed.getDate(),
    parsed.getHours(), parsed.getMinutes(), parsed.getSeconds(),
  );
  return new Date(utcMs).toISOString().replace('.000Z', 'Z');
}

// --- Notification Parsing ---

async function getNotification(
  page: Page,
  previousNotificationId?: string,
): Promise<Result<string, ErrorResponse>> {
  const baseSelector = previousNotificationId
    ? `[role='alert'].notification:not([data-notification-id='${previousNotificationId}'])`
    : "[role='alert'].notification";

  await page.locator(baseSelector).waitFor({ state: 'visible', timeout: TIMEOUT });

  const successNotification = page.locator(`[role='alert'].notification.success`);
  if ((await successNotification.count()) > 0) {
    const text = (await successNotification.textContent({ timeout: TIMEOUT }))?.trim() || '';
    return success(text);
  }

  const errorNotification = page.locator(`[role='alert'].notification.error`);
  const errorMessage =
    (await errorNotification.locator('.error-message').textContent({ timeout: TIMEOUT }))?.trim() || '';
  const fieldErrorTexts = await errorNotification.locator('.field-error').allTextContents();
  const fieldErrors = fieldErrorTexts.map((text) => {
    const colonIndex = text.indexOf(':');
    if (colonIndex >= 0) {
      return { field: text.substring(0, colonIndex).trim(), message: text.substring(colonIndex + 1).trim() };
    }
    return { field: '', message: text.trim() };
  });

  return failure({ message: errorMessage, fieldErrors });
}

// --- Shop UI Driver ---

export class ShopUiDriver implements ShopDriver {
  private context: BrowserContext | null = null;
  private page: Page | null = null;

  constructor(
    private baseUrl: string,
    private browser: Browser,
  ) {}

  async goToShop(): Promise<Result<void, ErrorResponse>> {
    try {
      this.context = await this.browser.newContext({ viewport: { width: 1920, height: 1080 } });
      this.page = await this.context.newPage();
      const response = await this.page.goto(this.baseUrl);
      if (response && response.status() === 200) {
        return success(undefined);
      }
      return failure({ message: `Shop UI not available: ${response?.status()}`, fieldErrors: [] });
    } catch (e) {
      return failure({ message: `Shop UI not available: ${e}`, fieldErrors: [] });
    }
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickNewOrder();

    const newOrderPage = new NewOrderPage(this.page!);
    await newOrderPage.fillSku(request.sku);
    if (request.quantity !== null) {
      await newOrderPage.fillQuantity(request.quantity);
    }
    if (request.country !== undefined && request.country !== null) {
      await newOrderPage.fillCountry(request.country);
    }
    if (request.couponCode) {
      await newOrderPage.fillCouponCode(request.couponCode);
    }
    await newOrderPage.clickPlaceOrder();

    const notificationResult = await getNotification(this.page!);
    if (notificationResult.success) {
      const orderNumber = NewOrderPage.getOrderNumber(notificationResult.value);
      if (orderNumber) {
        return success({ orderNumber });
      }
      return failure({ message: 'Could not extract order number from success message', fieldErrors: [] });
    }
    return failure(notificationResult.error);
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickOrderHistory();

    const orderHistoryPage = new OrderHistoryPage(this.page!);
    await orderHistoryPage.fillOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();
    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = new OrderDetailsPage(this.page!);
    await detailsPage.waitForLoad();

    return success({
      orderNumber: await detailsPage.getOrderNumber(),
      orderTimestamp: await detailsPage.getOrderTimestamp(),
      sku: await detailsPage.getSku(),
      quantity: await detailsPage.getQuantity(),
      unitPrice: await detailsPage.getUnitPrice(),
      basePrice: await detailsPage.getBasePrice(),
      discountRate: await detailsPage.getDiscountRate(),
      discountAmount: await detailsPage.getDiscountAmount(),
      subtotalPrice: await detailsPage.getSubtotalPrice(),
      taxRate: await detailsPage.getTaxRate(),
      taxAmount: await detailsPage.getTaxAmount(),
      totalPrice: await detailsPage.getTotalPrice(),
      country: await detailsPage.getCountry(),
      appliedCouponCode: await detailsPage.getAppliedCouponCode(),
      status: await detailsPage.getStatus(),
    });
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickOrderHistory();

    const orderHistoryPage = new OrderHistoryPage(this.page!);
    await orderHistoryPage.fillOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();
    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = new OrderDetailsPage(this.page!);
    await detailsPage.waitForLoad();
    await detailsPage.clickCancelOrder();

    const notificationResult = await getNotification(this.page!);
    if (notificationResult.success) {
      return success(undefined);
    }
    return failure(notificationResult.error);
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickOrderHistory();

    const orderHistoryPage = new OrderHistoryPage(this.page!);
    await orderHistoryPage.fillOrderNumber(orderNumber);
    await orderHistoryPage.clickSearch();
    await orderHistoryPage.clickViewOrderDetails(orderNumber);

    const detailsPage = new OrderDetailsPage(this.page!);
    await detailsPage.waitForLoad();
    await detailsPage.clickDeliverOrder();

    const notificationResult = await getNotification(this.page!);
    if (notificationResult.success) {
      return success(undefined);
    }
    return failure(notificationResult.error);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickAdminCoupons();

    const adminPage = new AdminCouponsPage(this.page!);
    await adminPage.fillCouponCode(request.code);
    await adminPage.fillDiscountRate(request.discountRate);
    if (request.validFrom) {
      await adminPage.fillValidFrom(request.validFrom);
    }
    if (request.validTo) {
      await adminPage.fillValidTo(request.validTo);
    }
    if (request.usageLimit !== undefined && request.usageLimit !== null) {
      await adminPage.fillUsageLimit(Number(request.usageLimit));
    }
    await adminPage.clickCreateCoupon();

    const notificationResult = await getNotification(this.page!);
    if (notificationResult.success) {
      return success(undefined);
    }
    return failure(notificationResult.error);
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, ErrorResponse>> {
    if (!this.page) {
      const goResult = await this.goToShop();
      if (!goResult.success) return failure(goResult.error);
    }

    await this.page!.goto(this.baseUrl);
    const homePage = new HomePage(this.page!);
    await homePage.clickAdminCoupons();

    const adminPage = new AdminCouponsPage(this.page!);
    await adminPage.clickRefreshCouponList();
    const rows = await adminPage.getCouponRows();

    return success({ coupons: rows });
  }

  async close(): Promise<void> {
    if (this.page) {
      await this.page.close().catch(() => {});
      this.page = null;
    }
    if (this.context) {
      await this.context.close().catch(() => {});
      this.context = null;
    }
  }
}
