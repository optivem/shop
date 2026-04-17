import type { ShopDriver } from '../../../../../../driver/port/shop/shop-driver.js';
import { BaseUseCase } from '../../../../shared/base-use-case.js';

export abstract class BaseShopUseCase<TResponse, TVerification> extends BaseUseCase<
  ShopDriver,
  TResponse,
  TVerification
> {}
