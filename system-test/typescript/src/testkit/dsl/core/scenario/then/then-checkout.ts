import { expect } from '@playwright/test';
import { Converter } from '../../../../common/converter.js';
import { CART, CHECKOUT, CONFIRMATION } from '../../../../driver/port/dtos/CheckOutResponse.js';
import type { BrowseCouponItem } from '../../../../driver/port/dtos/BrowseCouponsResponse.js';
import type { ViewOrderResponse } from '../../../../driver/port/dtos/ViewOrderResponse.js';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import { CheckOut } from '../../usecase/usecases/CheckOut.js';
import type { CheckOutVerification } from '../../usecase/usecases/CheckOutVerification.js';
import { ManageCart } from '../../usecase/usecases/ManageCart.js';
import { ViewOrderVerification } from '../../usecase/usecases/ViewOrderVerification.js';
import type { ErrorVerification } from '../../shared/error-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import { StagedProducts } from '../given/staged-products.js';
import type { ScenarioContext } from '../scenario-context.js';

const REVIEW_NOTE = 'Discount and tax are worked out per order when it is placed.';
const THANKS = 'Thank you for your order!';

/** One of the customer's steps through the checkout, set on the use case. */
export type CheckoutStep = (checkOut: CheckOut) => CheckOut;

/** What the When step asked the customer to do. */
export interface CheckoutSteps {
  cartLines: { skuAlias: string; quantity: string }[];
  /** After the cart is filled, the ERP's price of this product changes. */
  changedPrice: { skuAlias: string; unitPrice: string } | null;
  /** After the cart is filled, the ERP no longer offers this product. */
  withdrawnSkuAlias: string | null;
  steps: CheckoutStep[];
}

type CheckoutAssertion = (verification: CheckOutVerification) => void | Promise<void>;
type ErrorAssertion = (verification: ErrorVerification) => void;

/**
 * Checks out. The checkout shows only the products the scenario staged, so only the declared givens are arranged (no
 * default product or tax rate). When the ERP changes after the cart is filled, filling the cart and the checkout are
 * separate calls in one browser session.
 *
 * The confirmation shows order numbers only; which product each order is for is looked up on the order itself, the
 * way the confirmation's links lead to it.
 */
export class ThenCheckoutResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _assertions: CheckoutAssertion[] = [];
  private readonly _errorAssertions: ErrorAssertion[] = [];
  private readonly _orders = new Map<string, ViewOrderResponse>();
  /** The SKU of each confirmed order, by order number, once looked up. */
  private _placedSkus: Map<string, string> | null = null;
  private _savedProfile: ViewProfileResponse | null = null;
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    readonly useCaseContext: UseCaseContext,
    private readonly checkout: CheckoutSteps,
  ) {}

  shouldSucceed(): ThenCheckoutSuccess {
    this._expectSuccess = true;
    return new ThenCheckoutSuccess(this);
  }

  shouldFail(): ThenCheckoutFailure {
    this._expectSuccess = false;
    return new ThenCheckoutFailure(this);
  }

  _addAssertion(fn: CheckoutAssertion): void {
    this._assertions.push(fn);
  }

  _addErrorAssertion(fn: ErrorAssertion): void {
    this._errorAssertions.push(fn);
  }

  /** The SKU of each confirmed order, in the order shown, as the order itself shows it; each order is looked up once. */
  async _placedSkusOf(verification: CheckOutVerification): Promise<Map<string, string>> {
    if (this._placedSkus === null) {
      const orderNumbers: string[] = [];
      verification.forEachConfirmedOrder((orderNumber) => orderNumbers.push(orderNumber));
      const placed = new Map<string, string>();
      for (const orderNumber of orderNumbers) {
        placed.set(orderNumber, (await this._order(orderNumber)).sku);
      }
      this._placedSkus = placed;
    }
    return this._placedSkus;
  }

  /** The order the checkout placed for this product, as the system shows it now. */
  async _orderFor(verification: CheckOutVerification, skuAlias: string): Promise<ViewOrderResponse> {
    const placedSkus = await this._placedSkusOf(verification);
    const sku = this.useCaseContext.getParamValue(skuAlias);
    for (const [orderNumber, placedSku] of placedSkus) {
      if (placedSku === sku) return this._order(orderNumber);
    }
    throw new Error(`The confirmation shows no order for ${skuAlias}; it shows orders for ${JSON.stringify([...placedSkus.values()])}`);
  }

  /** The customer's profile as saved, read once. */
  async _profile(): Promise<ViewProfileResponse> {
    if (this._savedProfile) return this._savedProfile;
    const result = await this.app.myShop().viewProfile({});
    expect(result.success, JSON.stringify(result)).toBe(true);
    if (!result.success) throw new Error('The profile could not be read');
    this._savedProfile = result.value;
    return result.value;
  }

  /** Only an admin can browse the coupons, whoever the scenario acts as (here the customer who checked out). */
  async _coupon(couponCode: string): Promise<BrowseCouponItem> {
    const resolvedCouponCode = this.useCaseContext.getParamValue(couponCode);
    const result = await this.app.actingAs(UserIdentity.ADMIN, () => this.app.myShop().browseCoupons({}));
    expect(result.success, JSON.stringify(result)).toBe(true);
    if (!result.success) throw new Error('The coupons could not be browsed');
    const coupon = result.value.coupons.find((c) => c.code === resolvedCouponCode);
    if (!coupon) throw new Error(`Coupon '${resolvedCouponCode}' not found in browse results`);
    return coupon;
  }

  private async _order(orderNumber: string): Promise<ViewOrderResponse> {
    const cached = this._orders.get(orderNumber);
    if (cached) return cached;
    const result = await this.app.myShop().viewOrder({ orderNumber });
    expect(result.success, JSON.stringify(result)).toBe(true);
    if (!result.success) throw new Error(`Order ${orderNumber} could not be viewed`);
    this._orders.set(orderNumber, result.value);
    return result.value;
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext, { declaredOnly: true });

    const { cartLines, changedPrice, withdrawnSkuAlias } = this.checkout;
    const erpChanges = changedPrice !== null || withdrawnSkuAlias !== null;
    let checkOut = new CheckOut(this.app.myShop('dynamic'), this.useCaseContext);
    if (erpChanges) {
      const fillCart = new ManageCart(this.app.myShop('dynamic'), this.useCaseContext);
      for (const line of cartLines) fillCart.cartLine(line.skuAlias, line.quantity);
      (await fillCart.execute()).shouldSucceed();
      const stagedProducts = new StagedProducts(this.app, this.ctx, this.useCaseContext);
      if (changedPrice !== null) await stagedProducts.changePrice(changedPrice.skuAlias, changedPrice.unitPrice);
      if (withdrawnSkuAlias !== null) await stagedProducts.withdraw(withdrawnSkuAlias);
      checkOut.continueSession();
    } else {
      for (const line of cartLines) checkOut.cartLine(line.skuAlias, line.quantity);
    }
    for (const step of this.checkout.steps) checkOut = step(checkOut);
    const result = await checkOut.execute();

    if (this._expectSuccess) {
      const verification = result.shouldSucceed();
      for (const fn of this._assertions) await fn(verification);
    } else {
      const verification = result.shouldFail();
      for (const fn of this._errorAssertions) fn(verification);
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

/** Every step of the result: chains back to the result, and runs the scenario when awaited. */
abstract class BaseThenCheckoutStep implements PromiseLike<void> {
  constructor(protected readonly stage: ThenCheckoutResultStage) {}

  and(): ThenCheckoutSuccess {
    return new ThenCheckoutSuccess(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCheckoutSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenCheckoutResultStage) {}

  and(): this {
    return this;
  }

  checkout(): ThenCheckout {
    return new ThenCheckout(this.stage);
  }

  orderFor(skuAlias: string): ThenCheckedOutOrder {
    return new ThenCheckedOutOrder(this.stage, skuAlias);
  }

  profile(): ThenCheckedOutProfile {
    return new ThenCheckedOutProfile(this.stage);
  }

  coupon(couponCode: string): ThenCheckedOutCoupon {
    return new ThenCheckedOutCoupon(this.stage, couponCode);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCheckoutFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenCheckoutResultStage) {}

  and(): this {
    return this;
  }

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((v) => void v.errorMessage(expected));
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenCheckout extends BaseThenCheckoutStep {
  private add(fn: CheckoutAssertion): this {
    this.stage._addAssertion(fn);
    return this;
  }

  isShown(): this {
    return this.add((v) => void v.shownPage(CHECKOUT));
  }

  showsTheCart(): this {
    return this.add((v) => void v.shownPage(CART));
  }

  hasReviewLines(...skuAliases: string[]): this {
    return this.add((v) => void v.reviewLines(...skuAliases));
  }

  hasReviewLine(skuAlias: string, expectedQuantity: number, expectedLineTotal: string): this {
    return this.add((v) => void v.reviewQuantity(skuAlias, expectedQuantity).reviewLineTotal(skuAlias, expectedLineTotal));
  }

  hasReviewQuantity(skuAlias: string, expectedQuantity: number): this {
    return this.add((v) => void v.reviewQuantity(skuAlias, expectedQuantity));
  }

  hasReviewSubtotal(expectedSubtotal: string): this {
    return this.add((v) => void v.reviewSubtotal(expectedSubtotal));
  }

  hadTaxWhenOpened(expectedCountry: string, expectedRate: string): this {
    return this.add((v) => void v.openedTax(expectedCountry, expectedRate));
  }

  hasTax(expectedCountry: string, expectedRate: string): this {
    return this.add((v) => void v.tax(expectedCountry, expectedRate));
  }

  saysDiscountAndTaxAreWorkedOutPerOrder(): this {
    return this.add((v) => void v.reviewNote(REVIEW_NOTE));
  }

  hasSavedAddressSelected(expectedAddress: string): this {
    return this.add((v) => void v.savedAddressSelected(expectedAddress));
  }

  showsMessage(expectedMessage: string): this {
    return this.add((v) => void v.message(expectedMessage));
  }

  showsMessageDetail(expectedDetail: string): this {
    return this.add((v) => void v.messageDetail(expectedDetail));
  }

  saysRejected(skuAlias: string, expectedReason: string): this {
    return this.add((v) => void v.messageDetail(`${skuAlias}: ${expectedReason}`));
  }

  thanksForTheOrder(): this {
    return this.add((v) => void v.thanks(THANKS));
  }

  confirmsOrdersFor(...skuAliases: string[]): this {
    return this.add(async (v) => {
      v.shownPage(CONFIRMATION);
      v.placedFor(await this.stage._placedSkusOf(v), ...skuAliases);
    });
  }

  confirmsNoOrder(): this {
    return this.add((v) => void v.noOrderConfirmed());
  }

  linksEachOrderToItsDetails(): this {
    return this.add((v) => void v.eachOrderLinksToItsDetails());
  }

  offersViewMyOrdersAndContinueShopping(): this {
    return this.add((v) => void v.viewMyOrdersAndContinueShoppingOffered());
  }

  confirmsRejected(skuAlias: string, expectedReason: string): this {
    return this.add((v) => void v.rejectedLine(skuAlias, expectedReason));
  }

  confirmsNothingRejected(): this {
    return this.add((v) => void v.noRejectedLines());
  }

  hasCartLines(...skuAliases: string[]): this {
    return this.add((v) => void v.cartLines(...skuAliases));
  }

  hasCartLineQuantity(skuAlias: string, expectedQuantity: number): this {
    return this.add((v) => void v.cartLineQuantity(skuAlias, expectedQuantity));
  }
}

export class ThenCheckedOutOrder extends BaseThenCheckoutStep {
  constructor(
    stage: ThenCheckoutResultStage,
    private readonly skuAlias: string,
  ) {
    super(stage);
  }

  private add(fn: (order: ViewOrderResponse) => void): this {
    this.stage._addAssertion(async (v) => fn(await this.stage._orderFor(v, this.skuAlias)));
    return this;
  }

  hasQuantity(expectedQuantity: number): this {
    return this.add((order) => {
      expect(order.quantity, 'Order quantity').toBe(expectedQuantity);
    });
  }

  hasUnitPrice(expectedUnitPrice: string | number): this {
    return this.add((order) => {
      expect(Converter.normalizeDecimalOrNull(order.unitPrice), 'Order unit price').toBe(Converter.normalizeDecimal(expectedUnitPrice));
    });
  }

  hasTaxRate(expectedTaxRate: string | number): this {
    return this.add((order) => {
      expect(Converter.normalizeDecimalOrNull(order.taxRate), 'Order tax rate').toBe(Converter.normalizeDecimal(expectedTaxRate));
    });
  }

  hasDiscountRate(expectedDiscountRate: string | number): this {
    return this.add((order) => {
      expect(Converter.normalizeDecimalOrNull(order.discountRate), 'Order discount rate').toBe(Converter.normalizeDecimal(expectedDiscountRate));
    });
  }

  hasDiscountAmount(expectedDiscountAmount: string | number): this {
    return this.add((order) => {
      expect(Converter.normalizeDecimalOrNull(order.discountAmount), 'Order discount amount').toBe(
        Converter.normalizeDecimal(expectedDiscountAmount),
      );
    });
  }

  hasAppliedCoupon(expectedCouponCode?: string | null): this {
    return this.add((order) => {
      if (expectedCouponCode === undefined) {
        expect(order.appliedCouponCode, 'Order coupon').toBeDefined();
      } else {
        expect(order.appliedCouponCode ?? null, 'Order coupon').toBe(this.stage.useCaseContext.getParamValue(expectedCouponCode));
      }
    });
  }

  hasCountryCode(expectedCountry: string): this {
    return this.add((order) => {
      new ViewOrderVerification(order, this.stage.useCaseContext).countryCode(expectedCountry);
    });
  }

  hasShippingAddressWithCountryCode(firstName: string, lastName: string, street: string, postalCode: string, city: string, country: string): this {
    return this.add((order) => {
      new ViewOrderVerification(order, this.stage.useCaseContext).shippingAddressWithCountryCode({
        firstName,
        lastName,
        street,
        postalCode,
        city,
        country,
      });
    });
  }
}

/** Profile values are literals (the profile accepts only US, DE or GB), so nothing is alias-resolved. */
export class ThenCheckedOutProfile extends BaseThenCheckoutStep {
  hasFirstName(expected: string): this {
    return this.has('firstName', expected);
  }

  hasLastName(expected: string): this {
    return this.has('lastName', expected);
  }

  hasStreet(expected: string): this {
    return this.has('street', expected);
  }

  hasPostalCode(expected: string): this {
    return this.has('postalCode', expected);
  }

  hasCity(expected: string): this {
    return this.has('city', expected);
  }

  hasCountry(expected: string): this {
    return this.has('country', expected);
  }

  private has(field: keyof ViewProfileResponse, expected: string): this {
    this.stage._addAssertion(async () => {
      expect((await this.stage._profile())[field], `Profile ${field}`).toBe(expected);
    });
    return this;
  }
}

export class ThenCheckedOutCoupon extends BaseThenCheckoutStep {
  constructor(
    stage: ThenCheckoutResultStage,
    private readonly couponCode: string,
  ) {
    super(stage);
  }

  hasUsedCount(expectedUsedCount: number): this {
    this.stage._addAssertion(async () => {
      expect((await this.stage._coupon(this.couponCode)).usedCount, `Used count of coupon ${this.couponCode}`).toBe(expectedUsedCount);
    });
    return this;
  }
}
