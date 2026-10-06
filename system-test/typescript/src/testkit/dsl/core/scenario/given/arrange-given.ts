import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { DEFAULTS } from '../defaults.js';
import type { ScenarioContext } from '../scenario-context.js';
import { placeGivenOrder } from './place-given-order.js';

/**
 * Sets up everything the Given stage declared (clock, tax, promotion, products, coupons, orders), supplying the
 * defaults the given orders need, and then selects the identity the scenario action runs as.
 */
export async function arrangeGiven(app: AppContext, ctx: ScenarioContext, useCaseContext: UseCaseContext): Promise<void> {
  ctx.reserveDefaultCustomerIfUsed();

  if (ctx.clockConfig) {
    await app.clockDriver.returnsTime({ time: ctx.clockConfig.time });
  }

  const countryConfigs = ctx.countryConfigs.length > 0 ? ctx.countryConfigs : [{ country: DEFAULTS.COUNTRY, taxRate: DEFAULTS.TAX_RATE }];
  for (const countryConfig of countryConfigs) {
    const country = useCaseContext.getParamValueOrLiteral(countryConfig.country);
    await app.taxDriver.returnsTaxRate({ country, taxRate: countryConfig.taxRate });
  }

  await app.erpDriver.returnsPromotion({
    promotionActive: ctx.promotionConfig.promotionActive,
    discount: ctx.promotionConfig.discount,
  });

  const productConfigs = ctx.hasExplicitProduct ? ctx.productConfigs : [{ sku: DEFAULTS.SKU, price: DEFAULTS.UNIT_PRICE }];
  for (const productConfig of productConfigs) {
    const sku = useCaseContext.getParamValue(productConfig.sku);
    await app.erpDriver.returnsProduct({ sku, price: productConfig.price });
  }

  for (const couponConfig of ctx.couponConfigs) {
    await app.myShop().publishCoupon({
      code: useCaseContext.getParamValue(couponConfig.code),
      discountRate: String(couponConfig.discountRate),
      validFrom: couponConfig.validFrom,
      validTo: couponConfig.validTo,
      usageLimit: couponConfig.usageLimit === undefined ? undefined : String(couponConfig.usageLimit),
    });
  }

  for (const orderConfig of ctx.orderConfigs) {
    await placeGivenOrder(app, ctx, useCaseContext, orderConfig);
  }

  app.actAs(ctx.loggedInIdentity());
}
