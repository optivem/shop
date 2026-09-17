import type { ProductConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenProduct as IGivenProduct } from '../../../port/given/steps/given-product.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenProduct implements IGivenProduct {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: ProductConfig,
  ) {}

  withSku(sku: string): this {
    this.config.sku = sku;
    return this;
  }

  withUnitPrice(price: number | string): this {
    this.config.price = typeof price === 'number' ? price.toFixed(2) : price;
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
