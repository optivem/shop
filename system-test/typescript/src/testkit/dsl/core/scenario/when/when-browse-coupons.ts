import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseCouponsResultStage } from '../then/then-browse-coupons.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenBrowseCoupons {
  private page: string | null = null;
  private size: string | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withPage(page: string): this {
    this.page = page;
    return this;
  }

  withSize(size: string): this {
    this.size = size;
    return this;
  }

  then(): ThenBrowseCouponsResultStage;
  then(...args: unknown[]): ThenBrowseCouponsResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenBrowseCouponsResultStage(this.app, this.ctx, this.useCaseContext, this.page, this.size);
  }
}
