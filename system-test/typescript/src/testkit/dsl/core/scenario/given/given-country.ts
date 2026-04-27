import { CountryConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenCountry as IGivenCountry } from '../../../port/given/steps/given-country.js';

export class GivenCountry implements IGivenCountry {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: CountryConfig,
  ) {}

  withCode(country: string): this {
    this.config.country = country;
    return this;
  }

  withCountry(country: string): this {
    return this.withCode(country);
  }

  withTaxRate(taxRate: string | number): this {
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
