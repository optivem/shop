import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseOrderHistoryResultStage } from '../then/then-browse-order-history.js';
import { assertNotAwaited } from '../assert-not-awaited.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import { RESET_FILTERS } from '../../../../driver/port/dtos/BrowseOrderHistoryRequest.js';

export class WhenBrowseOrderHistory {
  private orderNumber: string = DEFAULTS.ORDER_NUMBER;
  private orderNumberPartOnly = false;
  private page: string | null = null;
  private size: string | null = null;
  private customer: (() => string) | null = null;
  private followUp: string | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withOrderNumber(orderNumber: string): this {
    this.orderNumber = orderNumber;
    this.orderNumberPartOnly = false;
    return this;
  }

  /**
   * Filters by part of the order number of the order known by this alias, in lower case: a few characters from its
   * middle, so it matches that order only, whatever the case.
   */
  withOrderNumberPartOf(orderNumber: string): this {
    this.orderNumber = orderNumber;
    this.orderNumberPartOnly = true;
    return this;
  }

  withNoOrderNumberFilter(): this {
    return this.withOrderNumber('');
  }

  withFiltersResetAfterwards(): this {
    this.followUp = RESET_FILTERS;
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
      this.followUp,
      this.orderNumberPartOnly,
    );
  }
}

function usernameOf(identity: UserIdentity): string {
  if (identity.kind !== 'CUSTOMER') {
    throw new Error(`Only a customer can be used as the order history customer filter, not ${identity.kind}`);
  }
  return UserIdentity.usernameOf(identity);
}
