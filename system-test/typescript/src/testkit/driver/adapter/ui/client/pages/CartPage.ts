import type { CartLineDto } from '../../../../port/dtos/ViewProductResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

const CART_LINES_SELECTOR = "table[aria-label='Cart Lines']";
const EMPTY_CART_SELECTOR = "p.lead:has-text('Your cart is empty.')";
export const CART_READY_SELECTOR = `${CART_LINES_SELECTOR}, ${EMPTY_CART_SELECTOR}`;
const CART_LINE_SELECTOR = `${CART_LINES_SELECTOR} tr[aria-label^='Cart Line ']`;
const CART_LINE_LABEL_PREFIX = 'Cart Line ';

/** The cart `/cart` (UC-008), read for its lines only. */
export class CartPage extends BasePage {
  async readLines(): Promise<CartLineDto[]> {
    await this.page.locator(CART_READY_SELECTOR).first().waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    const lines: CartLineDto[] = [];
    for (const row of await this.page.locator(CART_LINE_SELECTOR).all()) {
      const sku = ((await row.getAttribute('aria-label')) ?? '').substring(CART_LINE_LABEL_PREFIX.length);
      const quantity = await this.page
        .locator(`input[aria-label='Quantity for ${sku}']`)
        .inputValue({ timeout: PAGE_TIMEOUT_MS });
      lines.push({ sku, quantity: Number.parseInt(quantity.trim(), 10) });
    }
    return lines;
  }
}
