import type { ThenSuccess } from './steps/then-success.js';
import type { ThenPlaceOrderFailure } from './steps/then-place-order-failure.js';
import type { ThenResultStage } from './then-result-stage.js';

export interface ThenPlaceOrderResultStage extends ThenResultStage {
  shouldSucceed(): ThenSuccess;
  shouldFail(): ThenPlaceOrderFailure;
}
