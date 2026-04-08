import { PromotionConfig } from '../scenario-context.js';
import { ThenContractStage } from '../then/then-contract.js';
import { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';

export class GivenPromotion {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: PromotionConfig,
  ) {}

  withActive(promotionActive: boolean): GivenPromotion {
    this.config.promotionActive = promotionActive;
    return this;
  }

  withDiscount(discount: number | string): GivenPromotion {
    this.config.discount = typeof discount === 'number' ? discount.toFixed(2) : discount;
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
