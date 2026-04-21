import type { ShopDriver } from '../../../../../driver/port/shop/shop-driver.js';
import type { PublishCouponRequest } from '../../../../../driver/port/shop/dtos/PublishCouponRequest.js';
import { UseCaseResult } from '../../../shared/use-case-result.js';
import { VoidVerification } from '../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';
import { BaseShopUseCase } from './base/BaseShopUseCase.js';

export class PublishCoupon extends BaseShopUseCase<void, VoidVerification> {
  private _couponCodeParamAlias?: string;
  private _discountRate: number | string = '';
  private _validFrom?: string;
  private _validTo?: string;
  private _usageLimit?: number | string;

  constructor(driver: ShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  couponCode(couponCodeParamAlias: string): this {
    this._couponCodeParamAlias = couponCodeParamAlias;
    return this;
  }

  discountRate(discountRate: number | string): this {
    this._discountRate = discountRate;
    return this;
  }

  validFrom(validFrom: string): this {
    this._validFrom = validFrom;
    return this;
  }

  validTo(validTo: string): this {
    this._validTo = validTo;
    return this;
  }

  usageLimit(usageLimit: number | string): this {
    this._usageLimit = usageLimit;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const couponCode = this.context.getParamValue(this._couponCodeParamAlias);

    const request: PublishCouponRequest = {
      code: couponCode ?? '',
      discountRate: this._discountRate,
      validFrom: this._validFrom,
      validTo: this._validTo,
      usageLimit: this._usageLimit,
    };

    const result = await this.driver.publishCoupon(request);

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
