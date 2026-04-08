import { CountryConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';

export class GivenCountry {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: CountryConfig,
  ) {}

  withCode(country: string): GivenCountry {
    this.config.country = country;
    return this;
  }

  withCountry(country: string): GivenCountry {
    return this.withCode(country);
  }

  withTaxRate(taxRate: string | number): GivenCountry {
    this.config.taxRate = typeof taxRate === 'number' ? taxRate.toString() : taxRate;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage {
    return this.stage.then();
  }
}
