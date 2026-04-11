import type { AssumeRunning } from './steps/assume-running.js';

export interface AssumeStage {
  shop(): AssumeRunning;
  erp(): AssumeRunning;
  tax(): AssumeRunning;
  clock(): AssumeRunning;
}
