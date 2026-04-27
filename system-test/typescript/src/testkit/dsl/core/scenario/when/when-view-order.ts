import { DEFAULTS } from '../defaults.js';
import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenViewOrderResultStage } from '../then/then-view-order.js';

export class WhenViewOrder {
  private orderNumber: string = DEFAULTS.ORDER_NUMBER;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withOrderNumber(orderNumber: string): this {
    this.orderNumber = orderNumber;
    return this;
  }

  then(): ThenViewOrderResultStage {
    return new ThenViewOrderResultStage(this.app, this.ctx, this.useCaseContext, this.orderNumber);
  }
}
