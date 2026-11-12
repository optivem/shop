import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { DefaultGivens } from './default-givens.js';
import { placeGivenOrder } from './place-given-order.js';
import { saveGivenProfile } from './save-given-profile.js';

export interface ArrangeGivenOptions {
  /** Stage only the declared givens, not the defaults an order action needs (used by profile actions). */
  declaredOnly?: boolean;
}

/**
 * Sets up everything the Given stage declared (a new customer, clock, tax, promotion, products, ERP availability, coupons, orders, profiles), supplying the
 * defaults the given orders need, and then selects the identity the scenario action runs as.
 */
export async function arrangeGiven(
  app: AppContext,
  ctx: ScenarioContext,
  useCaseContext: UseCaseContext,
  options: ArrangeGivenOptions = {},
): Promise<void> {
  if (ctx.newCustomerConfig && ctx.customers.needsNewCustomer()) {
    const { firstName, lastName, email } = ctx.newCustomerConfig;
    ctx.customers.setNewCustomer(await app.newCustomer(firstName, lastName, email));
  }
  ctx.reserveDefaultCustomerIfUsed();
  // Profile actions never touch the ERP or tax service: without given orders, only what was declared is staged.
  const declaredOnly = options.declaredOnly === true && ctx.orderConfigs.length === 0;

  if (ctx.clockConfig) {
    await app.clockDriver.returnsTime({ time: ctx.clockConfig.time });
  }

  for (const countryConfig of declaredOnly ? ctx.countryConfigs : DefaultGivens.countriesFor(ctx)) {
    const country = useCaseContext.getParamValueOrLiteral(countryConfig.country);
    await app.taxDriver.returnsTaxRate({ country, taxRate: countryConfig.taxRate });
  }

  await app.erpDriver.returnsPromotion({
    promotionActive: ctx.promotionConfig.promotionActive,
    discount: ctx.promotionConfig.discount,
  });

  for (const productConfig of declaredOnly ? (ctx.hasExplicitProduct ? ctx.productConfigs : []) : DefaultGivens.productsFor(ctx)) {
    const sku = useCaseContext.getParamValue(productConfig.sku);
    await app.erpDriver.returnsProduct({ ...productConfig, sku });
  }

  for (const productList of ctx.productListConfigs) {
    const products = productList.map((productConfig) => ({ ...productConfig, sku: useCaseContext.getParamValue(productConfig.sku) }));
    const staged = await app.erpDriver.returnsProducts({ products });
    if (!staged.success) {
      throw new Error(`Failed to stage the ERP product list: ${staged.error.message}`);
    }
  }

  if (ctx.erpSlow) {
    const slowdown = await app.erpDriver.returnsSlow();
    if (!slowdown.success) {
      throw new Error(`Failed to make the ERP slow: ${slowdown.error.message}`);
    }
  }

  if (ctx.erpUnavailable) {
    const outage = await app.erpDriver.returnsUnavailable();
    if (!outage.success) {
      throw new Error(`Failed to make the ERP unavailable: ${outage.error.message}`);
    }
  }

  for (const couponConfig of DefaultGivens.couponsFor(ctx)) {
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

  for (const profileConfig of ctx.profileConfigs) {
    await saveGivenProfile(app, ctx, profileConfig);
  }

  app.actAs(ctx.loggedInIdentity());
}
