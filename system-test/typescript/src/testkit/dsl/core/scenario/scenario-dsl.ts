import { AppContext } from './app-context.js';
import { ScenarioContext } from './scenario-context.js';
import { UseCaseContext } from '../shared/use-case-context.js';
import { AssumeStage } from './assume/assume-stage.js';
import { GivenStage } from './given/given-stage.js';
import { WhenStage } from './when/when-stage.js';
export class ScenarioDsl {
  private readonly useCaseContext: UseCaseContext;
  private executed = false;

  constructor(private readonly app: AppContext, useCaseContext: UseCaseContext) {
    this.useCaseContext = useCaseContext;
  }

  assume(): AssumeStage {
    return new AssumeStage(this.app);
  }

  given(): GivenStage {
    this.ensureNotExecuted();
    return new GivenStage(this.app, new ScenarioContext(), this.useCaseContext);
  }

  when(): WhenStage {
    this.ensureNotExecuted();
    return new WhenStage(this.app, new ScenarioContext(), this.useCaseContext);
  }

  markAsExecuted(): void {
    this.executed = true;
  }

  async close(): Promise<void> {
    await this.app.closeAll();
  }

  private ensureNotExecuted(): void {
    if (this.executed) {
      throw new Error(
        'Scenario has already been executed. ' +
          'Each test method should contain only ONE scenario execution (Given-When-Then). ' +
          'Split multiple scenarios into separate test methods.',
      );
    }
  }
}
