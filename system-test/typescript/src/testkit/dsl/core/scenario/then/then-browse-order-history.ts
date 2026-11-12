import { expect } from '@playwright/test';
import type {
  BrowseOrderHistoryOrderItem,
  BrowseOrderHistoryResponse,
} from '../../../../driver/port/dtos/BrowseOrderHistoryResponse.js';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';

const ALL_CUSTOMERS = 'All customers';

export class ThenBrowseOrderHistoryResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _historyAssertions: ((history: BrowseOrderHistoryResponse, useCaseContext: UseCaseContext) => void)[] = [];
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly orderNumber: string,
    private readonly page: string | null = null,
    private readonly size: string | null = null,
    private readonly customer: (() => string) | null = null,
    private readonly followUp: string | null = null,
  ) {}

  /** The username of the customer known in this scenario by the given alias; valid once the scenario is set up. */
  _customerUsername(alias: string): string {
    return UserIdentity.usernameOf(this.ctx.customers.resolve(alias));
  }

  shouldSucceed(): ThenBrowseOrderHistorySuccess {
    this._expectSuccess = true;
    return new ThenBrowseOrderHistorySuccess(this);
  }

  shouldFail(): ThenBrowseOrderHistoryFailure {
    this._expectSuccess = false;
    return new ThenBrowseOrderHistoryFailure(this);
  }

  _addHistoryAssertion(fn: (history: BrowseOrderHistoryResponse, useCaseContext: UseCaseContext) => void): void {
    this._historyAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext);

    const orderNumber = this.useCaseContext.getResultValue(this.orderNumber);
    const customer = this.customer ? this.customer() : null;
    const result = await this.app.myShop('dynamic').browseOrderHistory({
      orderNumber,
      page: this.page,
      size: this.size,
      customer,
      followUp: this.followUp,
    });

    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (result.success) {
        for (const fn of this._historyAssertions) fn(result.value, this.useCaseContext);
      }
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenBrowseOrderHistorySuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

  and(): this {
    return this;
  }

  orderHistory(): ThenBrowseOrderHistoryOrderHistory {
    return new ThenBrowseOrderHistoryOrderHistory(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseOrderHistoryOrderHistory implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

  and(): this {
    return this;
  }

  containsOrder(orderNumber: string): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const expected = useCaseContext.getResultValue(orderNumber);
      expect(listedOrderNumbers(history), `Order history should contain order '${expected}'`).toContain(expected);
    });
    return this;
  }

  doesNotContainOrder(orderNumber: string): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const unexpected = useCaseContext.getResultValue(orderNumber);
      expect(listedOrderNumbers(history), `Order history should not contain order '${unexpected}'`).not.toContain(unexpected);
    });
    return this;
  }

  isEmpty(): this {
    this.stage._addHistoryAssertion((history) => {
      expect(listedOrderNumbers(history), 'Order history page should be empty').toHaveLength(0);
    });
    return this;
  }

  hasTotalElements(expectedTotalElements: number): this {
    this.stage._addHistoryAssertion((history) => {
      expect(history.totalElements, 'Order history total elements').toBe(expectedTotalElements);
    });
    return this;
  }

  /** The listed order shows who placed it (a customer alias), its SKU (an alias), quantity and status. */
  showsOrder(orderNumber: string, customerAlias: string, skuAlias: string, expectedQuantity: number, expectedStatus: OrderStatus): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const order = listedOrder(history, useCaseContext.getResultValue(orderNumber));
      expect(order.customer, `Customer of order '${order.orderNumber}'`).toBe(this.stage._customerUsername(customerAlias));
      expect(order.sku, `SKU of order '${order.orderNumber}'`).toBe(useCaseContext.getParamValue(skuAlias));
      expect(order.quantity, `Quantity of order '${order.orderNumber}'`).toBe(expectedQuantity);
      expect(order.status, `Status of order '${order.orderNumber}'`).toBe(expectedStatus);
    });
    return this;
  }

  /** The listed order links to its details page (UI only). */
  linksOrderToItsDetails(orderNumber: string): this {
    this.stage._addHistoryAssertion((history, useCaseContext) => {
      const order = listedOrder(history, useCaseContext.getResultValue(orderNumber));
      const link = order.detailsPath ?? '';
      expect(link, `Details link of order '${order.orderNumber}'`).toContain('/order-details');
      expect(link, `Details link of order '${order.orderNumber}'`).toContain(encodeURIComponent(order.orderNumber));
    });
    return this;
  }

  /** The customer filter offers All customers and these customers (aliases), among the others who ordered (UI only). */
  offersCustomers(...customerAliases: string[]): this {
    this.stage._addHistoryAssertion((history) => {
      const options = history.customerFilterOptions ?? [];
      expect(options, 'Customer filter choices').toContain(ALL_CUSTOMERS);
      for (const alias of customerAliases) {
        expect(options, 'Customer filter choices').toContain(this.stage._customerUsername(alias));
      }
    });
    return this;
  }

  /** The customer filter is set to All customers (UI only). */
  hasCustomerFilterSetToAllCustomers(): this {
    this.stage._addHistoryAssertion((history) => {
      expect(history.customerFilterSelection, 'Customer filter choice').toBe(ALL_CUSTOMERS);
    });
    return this;
  }

  /** There is no customer filter (UI only). */
  offersNoCustomerFilter(): this {
    this.stage._addHistoryAssertion((history) => {
      expect(history.customerFilterOptions ?? null, 'Customer filter choices').toBeNull();
    });
    return this;
  }

  /** The list has no Customer column (UI only). */
  showsNoCustomers(): this {
    this.stage._addHistoryAssertion((history) => {
      expect(history.customerColumnShown, 'Customer column shown').toBe(false);
    });
    return this;
  }

  /** The list shows this message instead of orders, such as "No orders found" (UI only). */
  showsMessage(expectedMessage: string): this {
    this.stage._addHistoryAssertion((history) => {
      expect(history.message, 'Order list message').toBe(expectedMessage);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseOrderHistoryFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseOrderHistoryResultStage) {}

  and(): this {
    return this;
  }

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  noOrderPersisted(): this {
    this.stage._addErrorAssertion((error) => {
      expect(error, 'Expected the rejected operation to yield an error response (nothing persisted)').toBeDefined();
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      const expandedMessage = useCaseContext.expandAliases(message);
      const fieldError = error.fieldErrors.find((fe) => fe.field === field);
      expect(fieldError?.message, `Expected field error for '${field}' in ${JSON.stringify(error.fieldErrors)}`).toBe(expandedMessage);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

function listedOrder(history: BrowseOrderHistoryResponse, orderNumber: string): BrowseOrderHistoryOrderItem {
  const order = history.orders.find((listed) => listed.orderNumber === orderNumber);
  if (!order) {
    throw new Error(`Order history should list order '${orderNumber}', but lists ${JSON.stringify(listedOrderNumbers(history))}`);
  }
  return order;
}

function listedOrderNumbers(history: BrowseOrderHistoryResponse): string[] {
  expect(history.orders, 'Orders list should not be null').toBeDefined();
  return history.orders.map((order) => order.orderNumber);
}
