import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseCustomersResultStage } from '../then/then-browse-customers.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

/** Asks for the customers who have placed orders (admin only; API channel). */
export class WhenBrowseCustomers {
  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  then(): ThenBrowseCustomersResultStage;
  then(...args: unknown[]): ThenBrowseCustomersResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenBrowseCustomersResultStage(this.app, this.ctx, this.useCaseContext);
  }
}
