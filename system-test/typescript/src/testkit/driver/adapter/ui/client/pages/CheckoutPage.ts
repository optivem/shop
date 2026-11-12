import type { CheckOutResponse, CheckOutReviewLineDto } from '../../../../port/dtos/CheckOutResponse.js';
import type { ShippingAddress } from '../../../../port/dtos/ShippingAddress.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';
import { CHECKOUT_CONFIRMATION_READY_SELECTOR } from './CheckoutConfirmationPage.js';

const PLACE_ORDER_SELECTOR = "button[aria-label='Place Checkout Order']";
/** The checkout has loaded (the single-page UIs load the profile first). */
export const CHECKOUT_READY_SELECTOR = PLACE_ORDER_SELECTOR;
const REVIEW_LINE_SELECTOR = "[aria-label='Order Review'] li[aria-label^='Review Line ']";
const REVIEW_LINE_LABEL_PREFIX = 'Review Line ';
const REVIEW_SUBTOTAL_SELECTOR = "[aria-label='Review Subtotal']";
const TAX_COUNTRY_SELECTOR = "[aria-label='Tax Country']";
const ESTIMATED_TAX_RATE_SELECTOR = "[aria-label='Estimated Tax Rate']";
const REVIEW_NOTE_SELECTOR = "p:has-text('Discount and tax are worked out per order')";
const USE_SAVED_ADDRESS_SELECTOR = "input[aria-label='Use Saved Address']";
const SAVED_ADDRESS_SELECTOR = "[aria-label='Saved Address']";
const SHIP_TO_DIFFERENT_ADDRESS_SELECTOR = "input[aria-label='Ship to a Different Address']";
const FIRST_NAME_SELECTOR = "input[aria-label='Shipping First Name']";
const LAST_NAME_SELECTOR = "input[aria-label='Shipping Last Name']";
const STREET_SELECTOR = "input[aria-label='Shipping Street']";
const POSTAL_CODE_SELECTOR = "input[aria-label='Shipping Postal Code']";
const CITY_SELECTOR = "input[aria-label='Shipping City']";
const COUNTRY_SELECTOR = "select[aria-label='Shipping Country']";
/** "Save as my default shipping address", offered on a first order only. */
const SAVE_AS_DEFAULT_SELECTOR = "input[aria-label='Save as Default Address']";
const COUPON_CODE_SELECTOR = "input[aria-label='Checkout Coupon Code']";
const ERROR_SELECTOR = "[role='alert'].notification.error";
const ERROR_MESSAGE_SELECTOR = `${ERROR_SELECTOR} .error-message`;
const ERROR_DETAIL_SELECTOR = `${ERROR_SELECTOR} .field-error`;

const reviewQuantitySelector = (sku: string) => `[aria-label='Review Quantity ${sku}']`;
const reviewLineTotalSelector = (sku: string) => `[aria-label='Review Line Total ${sku}']`;

/**
 * The checkout `/checkout` (UC-009): shipping address, optional coupon and the order review. The server-rendered UIs
 * post a form and render the checkout again, with an error, when nothing was placed; the single-page UIs stay in
 * place. The address errors are plain messages ("City is required") under the notification's message, and a rejected
 * line is "SKU: reason", so they are read as they are shown rather than parsed as `field: message`.
 */
export class CheckoutPage extends BasePage {
  async waitForLoaded(): Promise<void> {
    await this.page.locator(CHECKOUT_READY_SELECTOR).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  /** Chooses "Ship to a different address" (offered only with a saved address) and enters this one. */
  async inputShippingAddress(address: ShippingAddress): Promise<void> {
    if (await this.isVisible(SHIP_TO_DIFFERENT_ADDRESS_SELECTOR)) {
      await this.page.locator(SHIP_TO_DIFFERENT_ADDRESS_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
    }
    await this.fill(FIRST_NAME_SELECTOR, address.firstName);
    await this.fill(LAST_NAME_SELECTOR, address.lastName);
    await this.fill(STREET_SELECTOR, address.street);
    await this.fill(POSTAL_CODE_SELECTOR, address.postalCode);
    await this.fill(CITY_SELECTOR, address.city);
    await this.selectOptionByValueAddingIfMissing(COUNTRY_SELECTOR, address.country);
  }

  /** Ticks or unticks "Save as my default shipping address" (offered on a first order only). */
  async setSaveAsDefault(saveAsDefault: boolean): Promise<void> {
    await this.page.locator(SAVE_AS_DEFAULT_SELECTOR).first().setChecked(saveAsDefault, { timeout: PAGE_TIMEOUT_MS });
  }

  async inputCouponCode(couponCode: string): Promise<void> {
    await this.fill(COUPON_CODE_SELECTOR, couponCode);
  }

  /**
   * Places the order and waits for its outcome: the confirmation, or the checkout with an error. The checkout shows
   * no error before the order is placed, so either one is the outcome.
   */
  async clickPlaceOrder(): Promise<void> {
    await this.page.locator(PLACE_ORDER_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
    await this.page
      .locator(`${CHECKOUT_CONFIRMATION_READY_SELECTOR}, ${ERROR_SELECTOR}`)
      .first()
      .waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  async isShown(): Promise<boolean> {
    return this.isVisible(CHECKOUT_READY_SELECTOR);
  }

  async readTaxCountry(): Promise<string> {
    return this.text(TAX_COUNTRY_SELECTOR);
  }

  async readEstimatedTaxRate(): Promise<string> {
    return this.text(ESTIMATED_TAX_RATE_SELECTOR);
  }

  /** The checkout as shown now. */
  async readInto(response: CheckOutResponse): Promise<void> {
    await this.waitForLoaded();
    const lines: CheckOutReviewLineDto[] = [];
    for (const line of await this.page.locator(REVIEW_LINE_SELECTOR).all()) {
      const sku = ((await line.getAttribute('aria-label')) ?? '').substring(REVIEW_LINE_LABEL_PREFIX.length);
      lines.push({
        sku,
        quantity: Number.parseInt(await this.text(reviewQuantitySelector(sku)), 10),
        lineTotal: await this.text(reviewLineTotalSelector(sku)),
      });
    }
    response.reviewLines = lines;
    response.reviewSubtotal = await this.text(REVIEW_SUBTOTAL_SELECTOR);
    response.taxCountry = await this.readTaxCountry();
    response.estimatedTaxRate = await this.readEstimatedTaxRate();
    response.reviewNote = (await this.isVisible(REVIEW_NOTE_SELECTOR)) ? await this.text(REVIEW_NOTE_SELECTOR) : null;
    const hasSaved = await this.isVisible(USE_SAVED_ADDRESS_SELECTOR);
    response.savedAddressSelected = hasSaved && (await this.page.locator(USE_SAVED_ADDRESS_SELECTOR).first().isChecked());
    response.savedAddress = hasSaved ? await this.text(SAVED_ADDRESS_SELECTOR) : null;
    const formOpen = await this.isVisible(FIRST_NAME_SELECTOR);
    response.addressFormOpen = formOpen;
    response.shippingFirstName = formOpen ? await this.page.locator(FIRST_NAME_SELECTOR).first().inputValue() : null;
    response.shippingLastName = formOpen ? await this.page.locator(LAST_NAME_SELECTOR).first().inputValue() : null;
    response.saveAsDefaultTicked =
      (await this.isVisible(SAVE_AS_DEFAULT_SELECTOR)) && (await this.page.locator(SAVE_AS_DEFAULT_SELECTOR).first().isChecked());
    if (await this.isVisible(ERROR_SELECTOR)) {
      response.message = await this.text(ERROR_MESSAGE_SELECTOR);
      response.messageDetails = (await this.page.locator(ERROR_DETAIL_SELECTOR).allTextContents()).map((detail) => detail.trim());
    } else {
      response.messageDetails = [];
    }
  }

  private async fill(selector: string, value: string): Promise<void> {
    await this.page.locator(selector).fill(value, { timeout: PAGE_TIMEOUT_MS });
  }

  private async isVisible(selector: string): Promise<boolean> {
    return this.page.locator(selector).first().isVisible();
  }

  private async text(selector: string): Promise<string> {
    return ((await this.page.locator(selector).first().textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }
}
