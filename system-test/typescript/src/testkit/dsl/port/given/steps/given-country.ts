import type { GivenStep } from './given-step.js';

export interface GivenCountry extends GivenStep {
  withCode(country: string): GivenCountry;
  withTaxRate(taxRate: string | number): GivenCountry;
}
