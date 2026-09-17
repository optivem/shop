import { expect } from '@playwright/test';
import type { AppContext } from '../app-context.js';
import type { AssumeStage as IAssumeStage } from '../../../port/assume/assume-stage.js';
import type { AssumeRunning as IAssumeRunning } from '../../../port/assume/steps/assume-running.js';

export class AssumeStage implements IAssumeStage {
  constructor(private readonly app: AppContext) {}

  myShop(): AssumeRunning {
    return new AssumeRunning(this, async () => {
      const result = await this.app.myShop().goToMyShop({});
      expect(result.success, JSON.stringify(result)).toBe(true);
    });
  }

  erp(): AssumeRunning {
    return new AssumeRunning(this, async () => {
      const result = await this.app.erpDriver.goToErp();
      expect(result.success, JSON.stringify(result)).toBe(true);
    });
  }

  clock(): AssumeRunning {
    return new AssumeRunning(this, async () => {
      const result = await this.app.clockDriver.goToClock();
      expect(result.success, JSON.stringify(result)).toBe(true);
    });
  }

  tax(): AssumeRunning {
    return new AssumeRunning(this, async () => {
      const result = await this.app.taxDriver.goToTax();
      expect(result.success, JSON.stringify(result)).toBe(true);
    });
  }
}

export class AssumeRunning implements IAssumeRunning {
  constructor(
    private readonly stage: AssumeStage,
    private readonly checkFn: () => Promise<void>,
  ) {}

  async shouldBeRunning(): Promise<AssumeStage> {
    await this.checkFn();
    return this.stage;
  }
}
