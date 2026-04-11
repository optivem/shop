import type { GivenStep } from './given-step.js';

export interface GivenPromotion extends GivenStep {
  withActive(promotionActive: boolean): GivenPromotion;
  withDiscount(discount: string | number): GivenPromotion;
}
