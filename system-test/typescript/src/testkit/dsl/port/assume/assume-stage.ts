import type { AssumeRunning } from './steps/assume-running.js';

export interface AssumeStage {
  myShop(): AssumeRunning;
  erp(): AssumeRunning;
  tax(): AssumeRunning;
  clock(): AssumeRunning;
  identityProvider(): AssumeRunning;
}
