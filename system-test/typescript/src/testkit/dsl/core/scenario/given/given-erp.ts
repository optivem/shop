import type { ScenarioContext } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenErp as IGivenErp } from '../../../port/given/steps/given-erp.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

/** The ERP's availability; an unavailable ERP fails every call, whatever products or promotion are staged. */
export class GivenErp implements IGivenErp {
  constructor(
    private readonly stage: GivenStage,
    private readonly ctx: ScenarioContext,
  ) {}

  unavailable(): this {
    this.ctx.erpUnavailable = true;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage;
  then(...args: unknown[]): ThenContractStage {
    assertNotAwaited(args);
    return this.stage.then();
  }
}
