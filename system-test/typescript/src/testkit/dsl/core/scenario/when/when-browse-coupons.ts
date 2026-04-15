import { UseCaseContext } from '../../use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseCouponsResultStage } from '../then/then-browse-coupons.js';

export class WhenBrowseCoupons {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  then(): ThenBrowseCouponsResultStage {
    return new ThenBrowseCouponsResultStage(this.app, this.ctx, this.useCaseContext);
  }
}
