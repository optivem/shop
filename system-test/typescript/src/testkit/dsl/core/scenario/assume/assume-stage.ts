import { expect } from '@playwright/test';
import { AppContext } from '../app-context.js';

export class AssumeStage {
  constructor(private readonly app: AppContext) {}

  shop(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.shop().goToShop();
      expect(result.success).toBe(true);
    });
  }

  erp(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.erpDriver.goToErp();
      expect(result.success).toBe(true);
    });
  }

  clock(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.clockDriver.goToClock();
      expect(result.success).toBe(true);
    });
  }

  tax(): AssumeRunning {
    return new AssumeRunning(async () => {
      const result = await this.app.taxDriver.goToTax();
      expect(result.success).toBe(true);
    });
  }
}

export class AssumeRunning implements PromiseLike<void> {
  constructor(private readonly checkFn: () => Promise<void>) {}

  shouldBeRunning(): AssumeRunning {
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.checkFn().then(onfulfilled, onrejected);
  }
}
