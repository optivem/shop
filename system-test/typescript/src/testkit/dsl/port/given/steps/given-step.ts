import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenStep {
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
