import type { ThenStep } from './then-step.js';

export interface ThenCountry extends ThenStep<ThenCountry> {
  hasCountry(country: string): ThenCountry;
  hasTaxRate(taxRate: number): ThenCountry;
  hasTaxRateIsPositive(): ThenCountry;
}
