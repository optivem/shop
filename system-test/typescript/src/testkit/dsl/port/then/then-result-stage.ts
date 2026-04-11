import type { ThenStage } from './then-stage.js';
import type { ThenSuccess } from './steps/then-success.js';
import type { ThenFailure } from './steps/then-failure.js';

export interface ThenResultStage extends ThenStage {
  shouldSucceed(): ThenSuccess;
  shouldFail(): ThenFailure;
}
