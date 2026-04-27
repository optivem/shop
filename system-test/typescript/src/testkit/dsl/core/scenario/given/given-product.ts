import { ProductConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenProduct as IGivenProduct } from '../../../port/given/steps/given-product.js';

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

  then(): ThenContractStage {
    return this.stage.then();
  }
}
