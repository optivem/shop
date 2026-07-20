// Frontend DSL — the semantic "what the frontend does and sees" surface for the
// "latest" contract specs. It reads the same whether it drives the rendered UI
// (component spec) or the gateway directly (integration spec): the difference is
// absorbed by the swappable FrontendDriver the harness hands in.
//
// hasConfirmation/showsOrder/… are SEMANTIC — "a confirmation carrying order
// number X", not a literal UI string. Each driver realizes them its own way
// (screen text vs. a gateway result), so the DSL never hard-codes UI formatting.
//
// The DSL also owns the act-time binding: it takes a driver FACTORY and a backend-URL
// supplier, and on the first gesture it boots the stubbed backend, builds a fresh
// driver, and points it at that backend. That's what lets a spec go straight from
// backend.* (arrange) to frontend.* (act) with no mock-server URL in sight — the
// backend can't boot until the arrange phase is done staging, so first-gesture is
// exactly the right moment.
import type { FrontendDriver } from './driver/port/frontend-driver';
import type { OrderDetailExpectation } from './driver/port/dtos/OrderDetailExpectation';
import type { OrderHistoryRowExpectation } from './driver/port/dtos/OrderHistoryRowExpectation';

// Resolved on the first gesture, memoized for the rest of the test, dropped by reset().
type DriverHandle = () => Promise<FrontendDriver>;

export class FrontendDsl {
  private driver?: Promise<FrontendDriver>;

  constructor(
    private readonly newDriver: () => FrontendDriver,
    private readonly backendUrl: () => Promise<string>,
  ) {}

  // Called by the harness between tests: the next gesture builds a fresh driver
  // against a freshly booted backend.
  reset(): void {
    this.driver = undefined;
  }

  placeOrder(): PlaceOrderCommand {
    return new PlaceOrderCommand(this.handle());
  }

  browseOrderHistory(): BrowseOrderHistoryCommand {
    return new BrowseOrderHistoryCommand(this.handle());
  }

  browseCoupons(): BrowseCouponsCommand {
    return new BrowseCouponsCommand(this.handle());
  }

  viewOrderDetails(orderNumber: string): ViewOrderDetailsCommand {
    return new ViewOrderDetailsCommand(this.handle(), orderNumber);
  }

  cancelOrder(orderNumber: string): CancelOrderCommand {
    return new CancelOrderCommand(this.handle(), orderNumber);
  }

  publishCoupon(): PublishCouponCommand {
    return new PublishCouponCommand(this.handle());
  }

  private handle(): DriverHandle {
    return () => this.ready();
  }

  private ready(): Promise<FrontendDriver> {
    if (!this.driver) {
      this.driver = (async () => {
        const baseUrl = await this.backendUrl();
        const driver = this.newDriver();
        driver.useBackend(baseUrl);
        return driver;
      })();
    }
    return this.driver;
  }
}

class PlaceOrderCommand {
  private sku = 'BOOK-123';
  private quantity: number | string = 2;
  private country = 'US';
  private couponCode?: string;

  constructor(private readonly driver: DriverHandle) {}

  withSku(sku: string): this {
    this.sku = sku;
    return this;
  }

  withQuantity(quantity: number | string): this {
    this.quantity = quantity;
    return this;
  }

  withCountry(country: string): this {
    this.country = country;
    return this;
  }

  withCoupon(couponCode: string): this {
    this.couponCode = couponCode;
    return this;
  }

  execute(): PlaceOrderOutcome {
    const gesture = this.driver().then((driver) =>
      driver.placeOrder({
        sku: this.sku,
        quantity: this.quantity,
        country: this.country,
        couponCode: this.couponCode,
      }),
    );
    return new PlaceOrderOutcome(this.driver, gesture);
  }
}

class PlaceOrderOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async hasConfirmation(orderNumber: string): Promise<void> {
    await this.gesture;
    await (await this.driver()).hasConfirmation(orderNumber);
  }

  async hasError(message: string): Promise<void> {
    await this.gesture;
    await (await this.driver()).hasError(message);
  }

  // "the message for THIS field", not "this string is somewhere on the page" — a field
  // error that renders against the wrong field is a real defect, and this is what catches it.
  async hasFieldError(field: string, message: string): Promise<void> {
    await this.gesture;
    await (await this.driver()).hasFieldError(field, message);
  }
}

class BrowseOrderHistoryCommand {
  constructor(private readonly driver: DriverHandle) {}

  execute(): BrowseOrderHistoryOutcome {
    return new BrowseOrderHistoryOutcome(
      this.driver,
      this.driver().then((driver) => driver.browseOrderHistory()),
    );
  }
}

class BrowseOrderHistoryOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async showsOrder(orderNumber: string, expected?: OrderHistoryRowExpectation): Promise<void> {
    await this.gesture;
    await (await this.driver()).showsOrder(orderNumber, expected);
  }
}

class BrowseCouponsCommand {
  constructor(private readonly driver: DriverHandle) {}

  execute(): BrowseCouponsOutcome {
    return new BrowseCouponsOutcome(
      this.driver,
      this.driver().then((driver) => driver.browseCoupons()),
    );
  }
}

class BrowseCouponsOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async showsCoupon(code: string): Promise<void> {
    await this.gesture;
    await (await this.driver()).showsCoupon(code);
  }
}

class ViewOrderDetailsCommand {
  constructor(
    private readonly driver: DriverHandle,
    private readonly orderNumber: string,
  ) {}

  execute(): ViewOrderDetailsOutcome {
    return new ViewOrderDetailsOutcome(
      this.driver,
      this.driver().then((driver) => driver.viewOrderDetails(this.orderNumber)),
    );
  }
}

class ViewOrderDetailsOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async showsOrderDetails(orderNumber: string, expected: OrderDetailExpectation): Promise<void> {
    await this.gesture;
    await (await this.driver()).showsOrderDetails(orderNumber, expected);
  }

  async showsCancelAndDeliverActions(): Promise<void> {
    await this.gesture;
    await (await this.driver()).showsCancelAndDeliverActions();
  }

  async hidesCancelAndDeliverActions(): Promise<void> {
    await this.gesture;
    await (await this.driver()).hidesCancelAndDeliverActions();
  }

  async showsNotFound(): Promise<void> {
    await this.gesture;
    await (await this.driver()).showsNotFound();
  }
}

// Cancelling starts on the order-details screen: the gesture opens the order and presses the
// action, so the DSL says "cancel order X" and the driver owns the two steps that takes.
class CancelOrderCommand {
  constructor(
    private readonly driver: DriverHandle,
    private readonly orderNumber: string,
  ) {}

  execute(): CancelOrderOutcome {
    return new CancelOrderOutcome(
      this.driver,
      this.driver().then((driver) => driver.cancelOrder(this.orderNumber)),
    );
  }
}

class CancelOrderOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async wasCancelled(): Promise<void> {
    await this.gesture;
    await (await this.driver()).wasCancelled();
  }

  async wasRejected(message: string): Promise<void> {
    await this.gesture;
    await (await this.driver()).cancelWasRejected(message);
  }
}

class PublishCouponCommand {
  private code = 'SAVE10';
  private discountRate = 0.2;

  constructor(private readonly driver: DriverHandle) {}

  withCode(code: string): this {
    this.code = code;
    return this;
  }

  withDiscountRate(discountRate: number): this {
    this.discountRate = discountRate;
    return this;
  }

  execute(): PublishCouponOutcome {
    return new PublishCouponOutcome(
      this.driver,
      this.driver().then((driver) => driver.publishCoupon(this.code, this.discountRate)),
    );
  }
}

class PublishCouponOutcome {
  constructor(
    private readonly driver: DriverHandle,
    private readonly gesture: Promise<void>,
  ) {}

  async succeeded(): Promise<void> {
    await this.gesture;
    await (await this.driver()).succeeded();
  }
}
