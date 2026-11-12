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

  async hasCustomerFilter(): Promise<boolean> {
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
      orders.push({
        orderNumber: cell(ORDER_NUMBER_HEADER) ?? '',
        customer: customer === NO_CUSTOMER ? undefined : customer,
        sku: cell(SKU_HEADER) ?? '',
        quantity: quantity === undefined ? 0 : Number.parseInt(quantity, 10),
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
