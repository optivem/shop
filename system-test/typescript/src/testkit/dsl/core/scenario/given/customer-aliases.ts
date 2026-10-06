import { UserIdentity } from '../../../../driver/port/user-identity.js';

/**
 * Scenario-local mapping from customer aliases to test customers.
 *
 * The default customer (the no-argument forms) is always customer1. When a scenario uses the default customer,
 * customer1 is reserved for it and aliases are assigned from the remaining customers; otherwise aliases are
 * assigned from customer1 onwards. Within the pool, aliases are assigned in order of first use, and the same alias
 * always resolves to the same customer.
 */
export class CustomerAliases {
  private readonly aliases: string[] = [];
  private defaultCustomerUsed = false;

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

  defaultCustomer(): UserIdentity {
    return UserIdentity.customer(0);
  }

  resolve(alias: string): UserIdentity {
    const index = this.aliases.indexOf(alias);
    if (index < 0) {
      throw new Error(`Customer alias '${alias}' was never declared in this scenario`);
    }
    const customerIndex = index + (this.defaultCustomerUsed ? 1 : 0);
    if (customerIndex >= UserIdentity.CUSTOMER_COUNT) {
      const needed = this.aliases.length + (this.defaultCustomerUsed ? 1 : 0);
      throw new Error(
        `Cannot resolve customer alias '${alias}': the scenario needs ${needed} distinct customers ` +
          `but only ${UserIdentity.CUSTOMER_COUNT} test customers exist`,
      );
    }
    return UserIdentity.customer(customerIndex);
  }
}
