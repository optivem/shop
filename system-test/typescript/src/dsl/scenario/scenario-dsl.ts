import { AppContext } from './app-context.js';
import { ScenarioContext } from './scenario-context.js';
import { UseCaseContext } from '../use-case-context.js';
import { AssumeStage } from './assume/assume-stage.js';
import { GivenStage } from './given/given-stage.js';
import { WhenStage } from './when/when-stage.js';

export class ScenarioDsl {
  private readonly useCaseContext: UseCaseContext;

  constructor(private readonly app: AppContext, useCaseContext: UseCaseContext) {
    this.useCaseContext = useCaseContext;
  }

  assume(): AssumeStage {
    return new AssumeStage(this.app);
  }

  given(): GivenStage {
    return new GivenStage(this.app, new ScenarioContext(), this.useCaseContext);
  }

  when(): WhenStage {
    return new WhenStage(this.app, new ScenarioContext(), this.useCaseContext);
  }

  async close(): Promise<void> {
    await this.app.closeAll();
  }
}
