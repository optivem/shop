import { DEFAULTS } from '../defaults.js';
import type { CountryConfig, CouponConfig, ProductConfig, ScenarioContext } from '../scenario-context.js';

/** Single owner of which default givens a scenario gets; values come only from DEFAULTS. */
export const DefaultGivens = {
  product(): ProductConfig {
    return { sku: DEFAULTS.SKU, price: DEFAULTS.UNIT_PRICE };
  },

  country(): CountryConfig {
    return { country: DEFAULTS.COUNTRY, taxRate: DEFAULTS.TAX_RATE };
  },

  coupon(): CouponConfig {
    return { code: DEFAULTS.COUPON_CODE, discountRate: Number(DEFAULTS.DISCOUNT_RATE) };
  },

  /** The products to set up: those declared, else the default one. */
  productsFor(ctx: ScenarioContext): ProductConfig[] {
    return ctx.hasExplicitProduct ? ctx.productConfigs : [DefaultGivens.product()];
  },

  /** The countries to set up: those declared, else the default one. */
  countriesFor(ctx: ScenarioContext): CountryConfig[] {
    return ctx.countryConfigs.length > 0 ? ctx.countryConfigs : [DefaultGivens.country()];
  },

  /** The coupons to publish: those declared, else the default one when orders exist. */
  couponsFor(ctx: ScenarioContext): CouponConfig[] {
    if (ctx.couponConfigs.length > 0 || ctx.orderConfigs.length === 0) return ctx.couponConfigs;
    return [DefaultGivens.coupon()];
  },
};
