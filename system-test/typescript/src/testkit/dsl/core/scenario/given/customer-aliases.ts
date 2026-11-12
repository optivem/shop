import { UserIdentity } from '../../../../driver/port/user-identity.js';

/**
 * Scenario-local mapping from customer aliases to test customers.
 *
 * The default customer (the no-argument forms) is always customer1. When a scenario uses the default customer,
 * customer1 is reserved for it and aliases are assigned from the remaining customers; otherwise aliases are
 * assigned from customer1 onwards. A scenario with a new customer has that one as its default customer, so its aliases
 * are assigned from customer1 onwards too. Within the pool, aliases are assigned in order of first use, and the same alias
 * always resolves to the same customer.
 */
export class CustomerAliases {
  private readonly aliases: string[] = [];
  private defaultCustomerUsed = false;
  private newDefaultCustomer = false;
  private newCustomer: UserIdentity | undefined;

  register(alias: string): void {
    if (alias.trim() === '') {
      throw new Error('Customer alias must not be blank');
    }
    if (!this.aliases.includes(alias)) {
      this.aliases.push(alias);
    }
  }

  reserveDefaultCustomer(): void {
    this.defaultCustomerUsed = true;
  }

  /** The default customer is a new one, not customer1, so customer1 is free for aliases. */
  useNewDefaultCustomer(): void {
    this.newDefaultCustomer = true;
  }

  /** Whether the new default customer still has to be created. */
  needsNewCustomer(): boolean {
    return this.newDefaultCustomer && this.newCustomer === undefined;
  }

  /** The new default customer, once created. */
  setNewCustomer(identity: UserIdentity): void {
    this.newCustomer = identity;
  }

  /** The default customer: the new customer when the scenario has one (once set up), else customer1. */
  defaultCustomer(): UserIdentity {
    if (!this.newDefaultCustomer) return UserIdentity.customer(0);
    if (this.newCustomer === undefined) {
      throw new Error('The new customer is created when the scenario is set up');
    }
    return this.newCustomer;
  }

  resolve(alias: string): UserIdentity {
    const index = this.aliases.indexOf(alias);
    if (index < 0) {
      throw new Error(`Customer alias '${alias}' was never declared in this scenario`);
    }
    const offset = this.defaultCustomerUsed && !this.newDefaultCustomer ? 1 : 0;
    const customerIndex = index + offset;
    if (customerIndex >= UserIdentity.CUSTOMER_COUNT) {
      const needed = this.aliases.length + offset;
      throw new Error(
        `Cannot resolve customer alias '${alias}': the scenario needs ${needed} distinct shared customers ` +
          `but only ${UserIdentity.CUSTOMER_COUNT} test customers exist`,
      );
    }
    return UserIdentity.customer(customerIndex);
  }
}
