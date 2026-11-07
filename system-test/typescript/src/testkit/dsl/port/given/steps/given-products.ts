import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenProducts {
  withProduct(sku: string, unitPrice: string | number): GivenProducts;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
