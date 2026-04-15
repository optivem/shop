import type { GivenStep } from './given-step.js';

export interface GivenProduct extends GivenStep {
  withSku(sku: string): GivenProduct;
  withUnitPrice(unitPrice: string | number): GivenProduct;
}
