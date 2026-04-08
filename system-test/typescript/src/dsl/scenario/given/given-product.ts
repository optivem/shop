import { ProductConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';

export class GivenProduct {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: ProductConfig,
  ) {}

  withSku(sku: string): GivenProduct {
    this.config.sku = sku;
    return this;
  }

  withUnitPrice(price: number | string): GivenProduct {
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
