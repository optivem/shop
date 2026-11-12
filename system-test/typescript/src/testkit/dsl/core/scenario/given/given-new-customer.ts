import type { NewCustomerConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenNewCustomer as IGivenNewCustomer } from '../../../port/given/steps/given-new-customer.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenNewCustomer implements IGivenNewCustomer {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: NewCustomerConfig,
  ) {}

  withFirstName(firstName: string): this {
    this.config.firstName = firstName;
    return this;
  }

  withLastName(lastName: string): this {
    this.config.lastName = lastName;
    return this;
  }

  withEmail(email: string): this {
    this.config.email = email;
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
