import type { AssumeStage } from '../assume-stage.js';

export interface AssumeRunning extends PromiseLike<void> {
  shouldBeRunning(): AssumeStage;
}
