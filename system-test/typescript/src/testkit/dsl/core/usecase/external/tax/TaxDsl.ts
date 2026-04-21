import type { TaxDriver } from '../../../../../driver/port/external/tax/tax-driver.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';
import { GoToTax } from './usecases/GoToTax.js';
import { GetTaxRate } from './usecases/GetTaxRate.js';
import { ReturnsTaxRate } from './usecases/ReturnsTaxRate.js';

export class TaxDsl {
  constructor(
    private readonly driver: TaxDriver,
    private readonly context: UseCaseContext,
  ) {}

  goToTax(): GoToTax {
    return new GoToTax(this.driver, this.context);
  }

  getTaxRate(): GetTaxRate {
    return new GetTaxRate(this.driver, this.context);
  }

  returnsTaxRate(): ReturnsTaxRate {
    return new ReturnsTaxRate(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
