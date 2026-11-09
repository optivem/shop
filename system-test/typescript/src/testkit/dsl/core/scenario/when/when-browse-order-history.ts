import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseOrderHistoryResultStage } from '../then/then-browse-order-history.js';
import { assertNotAwaited } from '../assert-not-awaited.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';

export class WhenBrowseOrderHistory {
  private orderNumber: string = DEFAULTS.ORDER_NUMBER;
  private page: string | null = null;
  private size: string | null = null;
  private customer: (() => string) | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withOrderNumber(orderNumber: string): this {
    this.orderNumber = orderNumber;
    return this;
  }

  withPage(page: string): this {
    this.page = page;
    return this;
  }

  withSize(size: string): this {
    this.size = size;
    return this;
  }

  /**
   * Filters by the customer known in this scenario by the given alias, or by the default customer (customer1);
   * sent as that test customer's username. Resolved lazily, like the logged-in customer.
   */
  withCustomer(alias?: string): this {
    const customers = this.ctx.customers;
    if (alias === undefined) {
      customers.reserveDefaultCustomer();
      this.customer = () => usernameOf(customers.defaultCustomer());
    } else {
      customers.register(alias);
      this.customer = () => usernameOf(customers.resolve(alias));
    }
    return this;
  }

  then(): ThenBrowseOrderHistoryResultStage;
  then(...args: unknown[]): ThenBrowseOrderHistoryResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenBrowseOrderHistoryResultStage(
      this.app,
      this.ctx,
      this.useCaseContext,
      this.orderNumber,
      this.page,
      this.size,
      this.customer,
    );
  }
}

function usernameOf(identity: UserIdentity): string {
  if (identity.kind !== 'CUSTOMER') {
    throw new Error(`Only a customer can be used as the order history customer filter, not ${identity.kind}`);
  }
  return UserIdentity.customerUsername(identity.customerIndex);
}
