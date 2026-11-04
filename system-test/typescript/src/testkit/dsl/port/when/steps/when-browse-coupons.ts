import type { ThenResultStage } from '../../then/then-result-stage.js';

export interface WhenBrowseCoupons {
  withPage(page: string): this;
  withSize(size: string): this;
  then(): ThenResultStage;
}
