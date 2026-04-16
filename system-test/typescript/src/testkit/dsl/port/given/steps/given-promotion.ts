import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenPromotion {
  withActive(promotionActive: boolean): GivenPromotion;
  withDiscount(discount: string | number): GivenPromotion;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
