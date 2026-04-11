import type { ThenResultStage } from '../../then/then-result-stage.js';

export interface WhenStep {
  then(): ThenResultStage;
}
