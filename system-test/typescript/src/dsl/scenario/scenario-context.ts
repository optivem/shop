import { DEFAULTS } from '../defaults.js';

export interface ClockConfig {
  time: string;
}

export interface ProductConfig {
  sku: string;
  price: string;
}

export interface PromotionConfig {
  promotionActive: boolean;
  discount: string;
}

export interface CouponConfig {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number | string;
}

export interface CountryConfig {
  country: string;
  taxRate: string;
}

export interface OrderConfig {
  sku: string;
  quantity: string;
  country: string;
  couponCode: string | null;
  status: string;
  orderNumber?: string;
}

export class ScenarioContext {
  clockConfig: ClockConfig | null = null;
  productConfigs: ProductConfig[] = [];
  couponConfigs: CouponConfig[] = [];
  countryConfigs: CountryConfig[] = [];
  orderConfigs: OrderConfig[] = [];
  hasExplicitProduct = false;
  promotionConfig: PromotionConfig = { promotionActive: DEFAULTS.PROMOTION_ACTIVE, discount: DEFAULTS.PROMOTION_DISCOUNT };
  hasExplicitPromotion = false;
}
