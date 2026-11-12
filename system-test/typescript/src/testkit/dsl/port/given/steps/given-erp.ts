import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenErp {
  unavailable(): GivenErp;
  slow(): GivenErp;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
