import type { AppContext, ChannelMode } from '../scenario/app-context.js';
import { ShopDsl } from './shop/ShopDsl.js';
import { ClockDsl } from './external/clock/ClockDsl.js';
import { ErpDsl } from './external/erp/ErpDsl.js';
import { TaxDsl } from './external/tax/TaxDsl.js';

export class UseCaseDsl {
  private _shopDsl?: ShopDsl;
  private _clockDsl?: ClockDsl;
  private _erpDsl?: ErpDsl;
  private _taxDsl?: TaxDsl;

  constructor(private readonly app: AppContext) {}

  shop(mode?: ChannelMode): ShopDsl {
    if (!this._shopDsl || mode) {
      this._shopDsl = new ShopDsl(this.app.shop(mode));
    }
    return this._shopDsl;
  }

  clock(): ClockDsl {
    if (!this._clockDsl) {
      this._clockDsl = new ClockDsl(this.app.clockDriver);
    }
    return this._clockDsl;
  }

  erp(): ErpDsl {
    if (!this._erpDsl) {
      this._erpDsl = new ErpDsl(this.app.erpDriver);
    }
    return this._erpDsl;
  }

  tax(): TaxDsl {
    if (!this._taxDsl) {
      this._taxDsl = new TaxDsl(this.app.taxDriver);
    }
    return this._taxDsl;
  }

  async close(): Promise<void> {
    await this.app.closeAll();
  }
}
