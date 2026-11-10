import type { ProductConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenProducts as IGivenProducts } from '../../../port/given/steps/given-products.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

/** Stages the ERP's enumerable product list (GET /api/products). */
export class GivenProducts implements IGivenProducts {
  constructor(
    private readonly stage: GivenStage,
    private readonly products: ProductConfig[],
  ) {}

  withProduct(sku: string, unitPrice: string | number): this;
  withProduct(sku: string, title: string, brand: string, category: string, unitPrice: string, description: string): this;
  withProduct(sku: string, ...details: (string | number)[]): this {
    if (details.length === 1) {
      const unitPrice = details[0] ?? '';
      this.products.push({ sku, price: typeof unitPrice === 'number' ? unitPrice.toFixed(2) : unitPrice });
      return this;
    }
    const [title, brand, category, unitPrice, description] = details.map(String);
    this.products.push({ sku, title, brand, category, price: unitPrice ?? '', description });
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
