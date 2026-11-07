import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenProduct {
  withSku(sku: string): GivenProduct;
  withUnitPrice(unitPrice: string | number): GivenProduct;
  withTitle(title: string): GivenProduct;
  withDescription(description: string): GivenProduct;
  withCategory(category: string): GivenProduct;
  withBrand(brand: string): GivenProduct;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
