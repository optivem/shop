import type { BrowseOrderHistoryOrderItem } from '../../../../port/dtos/BrowseOrderHistoryResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

const ORDER_NUMBER_INPUT_SELECTOR = "[aria-label='Order Number']";
const SEARCH_BUTTON_SELECTOR = "[aria-label='Refresh Order List']";
const CUSTOMER_FILTER_SELECTOR = "select[aria-label='Customer Filter']";
/** A choice after "All customers": the customers have loaded. */
const LOADED_CUSTOMER_OPTION_SELECTOR = `${CUSTOMER_FILTER_SELECTOR} option:nth-child(2)`;
const CUSTOMER_OPTION_SELECTOR = `${CUSTOMER_FILTER_SELECTOR} option`;
const TABLE_ROW_SELECTOR = 'table tbody tr:visible';
const ORDER_NUMBER_CELL_SELECTOR = 'table tbody tr:visible:has(a) td:first-child';
const HEADER_SELECTOR = 'table:visible thead th';
const ORDER_ROW_SELECTOR = 'table:visible tbody tr:has(a)';
const EMPTY_MESSAGE_SELECTOR = '.orders-empty:visible';
const ORDER_NUMBER_HEADER = 'Order Number';
const CUSTOMER_HEADER = 'Customer';
const SKU_HEADER = 'SKU';
const QUANTITY_HEADER = 'Quantity';
const STATUS_HEADER = 'Status';
const ORDER_DATE_HEADER = 'Order Date';
const TOTAL_PRICE_HEADER = 'Total Price';
/** Every UI shows the order date as toLocaleString('en-US', { timeZone: 'UTC' }), e.g. "1/10/2026, 9:00:00 AM". */
const ORDER_DATE_PATTERN = /^(\d{1,2})\/(\d{1,2})\/(\d{4}), (\d{1,2}):(\d{2}):(\d{2})\s(AM|PM)$/;
const HOURS_PER_HALF_DAY = 12;

/** The order date as an ISO-8601 instant; browsers may put a narrow or a non-breaking space before AM/PM. */
function parseOrderDate(text: string): string {
  const match = ORDER_DATE_PATTERN.exec(text.replaceAll('\u202f', ' ').replaceAll('\u00a0', ' ').trim());
  if (!match) {
    throw new Error(`Unexpected order date format: '${text}'`);
  }
  const [, month, day, year, hour, minute, second, meridiem] = match.map((part) => part ?? '');
  const hourOfHalfDay = Number(hour) % HOURS_PER_HALF_DAY;
  const hourOfDay = meridiem === 'PM' ? hourOfHalfDay + HOURS_PER_HALF_DAY : hourOfHalfDay;
  return new Date(Date.UTC(Number(year), Number(month) - 1, Number(day), hourOfDay, Number(minute), Number(second))).toISOString();
}
const NO_CUSTOMER = '-';
const ALL_CUSTOMERS = 'All customers';

/**
 * The order history `/order-history`: My orders for a customer, All orders for an admin (UC-002, UC-011). An admin also
 * gets a customer filter, which offers the customers found in the order list once they have loaded, and a Customer
 * column. The cells are read by their column's header, which every UI names the same way.
 */
export class OrderHistoryPage extends BasePage {
  async inputOrderNumber(orderNumber: string): Promise<void> {
    await this.page.locator(ORDER_NUMBER_INPUT_SELECTOR).fill(orderNumber, { timeout: PAGE_TIMEOUT_MS });
  }

  async clickSearch(): Promise<void> {
    await this.page.locator(SEARCH_BUTTON_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
  }

  /**
   * Whether the page offers a customer filter (an admin's). Waits for the page's filter bar first: the order history
   * link navigates client-side, so right after the click the previous page may still be showing.
   */
  async hasCustomerFilter(): Promise<boolean> {
    await this.page.locator(ORDER_NUMBER_INPUT_SELECTOR).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
    return (await this.page.locator(CUSTOMER_FILTER_SELECTOR).count()) > 0;
  }

  /** Chooses this customer (a username), waiting until the filter offers it; the list reloads at once. */
  async selectCustomer(username: string): Promise<void> {
    await this.page.locator(CUSTOMER_FILTER_SELECTOR).selectOption({ label: username }, { timeout: PAGE_TIMEOUT_MS });
  }

  /** Sets the customer filter back to All customers; the list reloads at once. */
  async selectAllCustomers(): Promise<void> {
    await this.page.locator(CUSTOMER_FILTER_SELECTOR).selectOption({ label: ALL_CUSTOMERS }, { timeout: PAGE_TIMEOUT_MS });
  }

  /** Waits until the filter offers the customers, then reads its choices as shown. */
  async readCustomerFilterOptions(): Promise<string[]> {
    await this.page.locator(LOADED_CUSTOMER_OPTION_SELECTOR).first().waitFor({ state: 'attached', timeout: PAGE_TIMEOUT_MS });
    return (await this.page.locator(CUSTOMER_OPTION_SELECTOR).allTextContents()).map((option) => option.trim());
  }

  async readCustomerFilterSelection(): Promise<string | null> {
    const selection = this.page.locator(`${CUSTOMER_FILTER_SELECTOR} option:checked`);
    if ((await selection.count()) === 0) return null;
    return ((await selection.first().textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '').trim();
  }

  /** Reads the listed order numbers; the table holds a single linkless row when there are no orders. */
  async readOrderNumbers(): Promise<string[]> {
    await this.waitForList();
    const orderNumbers = await this.page.locator(ORDER_NUMBER_CELL_SELECTOR).allTextContents();
    return orderNumbers.map((orderNumber) => orderNumber.trim());
  }

  /** Reads the listed orders, each cell by its column's header; the table has a single linkless row when empty. */
  async readOrders(): Promise<BrowseOrderHistoryOrderItem[]> {
    await this.waitForList();
    const headers = await this.readHeaders();
    const orders: BrowseOrderHistoryOrderItem[] = [];
    for (const row of await this.page.locator(ORDER_ROW_SELECTOR).all()) {
      const cells = (await row.locator('td').allTextContents()).map((cell) => cell.trim());
      const cell = (header: string): string | undefined => {
        const column = headers.indexOf(header);
        return column < 0 ? undefined : cells[column];
      };
      const customer = cell(CUSTOMER_HEADER);
      const quantity = cell(QUANTITY_HEADER);
      const orderDate = cell(ORDER_DATE_HEADER);
      const totalPrice = cell(TOTAL_PRICE_HEADER);
      orders.push({
        orderNumber: cell(ORDER_NUMBER_HEADER) ?? '',
        customer: customer === NO_CUSTOMER ? undefined : customer,
        sku: cell(SKU_HEADER) ?? '',
        orderTimestamp: orderDate === undefined ? '' : parseOrderDate(orderDate),
        quantity: quantity === undefined ? 0 : Number.parseInt(quantity, 10),
        totalPrice: totalPrice === undefined ? '' : totalPrice.replace('$', '').trim(),
        status: cell(STATUS_HEADER) ?? '',
        detailsPath: await row.locator('a').first().getAttribute('href'),
      } as BrowseOrderHistoryOrderItem);
    }
    return orders;
  }

  async hasCustomerColumn(): Promise<boolean> {
    await this.waitForList();
    return (await this.readHeaders()).includes(CUSTOMER_HEADER);
  }

  /** The message shown instead of orders, such as "No orders found"; null when orders are listed. */
  async readMessage(): Promise<string | null> {
    await this.waitForList();
    const messages = await this.page.locator(EMPTY_MESSAGE_SELECTOR).allTextContents();
    return messages.length === 0 ? null : (messages[0] ?? '').trim();
  }

  async isOrderListed(orderNumber: string): Promise<boolean> {
    const row = this.page.locator(`//tr[contains(., '${orderNumber}')]`);
    try {
      await row.waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
      return true;
    } catch {
      return false;
    }
  }

  async clickViewOrderDetails(orderNumber: string): Promise<void> {
    const row = this.page.locator(`//tr[contains(., '${orderNumber}')]`);
    await row.locator("//a[contains(text(), 'View Details')]").click({ timeout: PAGE_TIMEOUT_MS });
  }

  /** The list has loaded: it shows a row, of orders or of its "No orders found" message. */
  private async waitForList(): Promise<void> {
    await this.page.locator(TABLE_ROW_SELECTOR).first().waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  private async readHeaders(): Promise<string[]> {
    return (await this.page.locator(HEADER_SELECTOR).allTextContents()).map((header) => header.trim());
  }
}
