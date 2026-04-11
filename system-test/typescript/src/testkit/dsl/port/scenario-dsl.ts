import type { AssumeStage } from './assume/assume-stage.js';
import type { GivenStage } from './given/given-stage.js';
import type { WhenStage } from './when/when-stage.js';

export interface ScenarioDsl {
  assume(): AssumeStage;
  given(): GivenStage;
  when(): WhenStage;
  close(): Promise<void>;
}
