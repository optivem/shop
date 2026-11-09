import type { ProfileConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { CustomerAliases } from './customer-aliases.js';
import type { GivenProfile as IGivenProfile } from '../../../port/given/steps/given-profile.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenProfile implements IGivenProfile {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: ProfileConfig,
    private readonly customers: CustomerAliases,
  ) {}

  withFirstName(firstName: string): this {
    this.config.firstName = firstName;
    return this;
  }

  withLastName(lastName: string): this {
    this.config.lastName = lastName;
    return this;
  }

  withStreet(street: string): this {
    this.config.street = street;
    return this;
  }

  withPostalCode(postalCode: string): this {
    this.config.postalCode = postalCode;
    return this;
  }

  withCity(city: string): this {
    this.config.city = city;
    return this;
  }

  withCountry(country: string): this {
    this.config.country = country;
    return this;
  }

  /** The profile is saved by the customer known in this scenario by the given alias, or by the default customer (customer1) when no alias is given. */
  savedByCustomer(alias?: string): this {
    if (alias === undefined) {
      this.config.savedByAlias = undefined;
    } else {
      this.customers.register(alias);
      this.config.savedByAlias = alias;
    }
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage;
  then(...args: unknown[]): ThenContractStage {
    assertNotAwaited(args);
    return this.stage.then();
  }
}
