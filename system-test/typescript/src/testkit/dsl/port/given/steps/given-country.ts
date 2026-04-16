import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenCountry {
  withCode(country: string): GivenCountry;
  withTaxRate(taxRate: string | number): GivenCountry;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
