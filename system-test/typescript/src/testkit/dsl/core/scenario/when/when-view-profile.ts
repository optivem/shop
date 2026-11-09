import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenProfileResultStage } from '../then/then-profile.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenViewProfile {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  then(): ThenProfileResultStage;
  then(...args: unknown[]): ThenProfileResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenProfileResultStage(this.app, this.ctx, this.useCaseContext, null);
  }
}
