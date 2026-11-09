import type { Browser } from '@playwright/test';
import type { Result } from '../../../common/result.js';
import { success, failure } from '../../../common/result.js';
import type { GoToMyShopRequest } from '../../port/dtos/GoToMyShopRequest.js';
import type { GoToMyShopResponse } from '../../port/dtos/GoToMyShopResponse.js';
import type { PlaceOrderRequest } from '../../port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../port/dtos/PlaceOrderResponse.js';
import type { CancelOrderRequest } from '../../port/dtos/CancelOrderRequest.js';
import type { CancelOrderResponse } from '../../port/dtos/CancelOrderResponse.js';
import type { DeliverOrderRequest } from '../../port/dtos/DeliverOrderRequest.js';
import type { DeliverOrderResponse } from '../../port/dtos/DeliverOrderResponse.js';
import type { ViewOrderRequest } from '../../port/dtos/ViewOrderRequest.js';
import type { ViewOrderResponse } from '../../port/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../port/dtos/errors/SystemError.js';
import type { BrowseOrderHistoryRequest } from '../../port/dtos/BrowseOrderHistoryRequest.js';
import type {
  BrowseOrderHistoryOrderItem,
  BrowseOrderHistoryResponse,
} from '../../port/dtos/BrowseOrderHistoryResponse.js';
import type { UserIdentity } from '../../port/user-identity.js';
import type { PublishCouponRequest } from '../../port/dtos/PublishCouponRequest.js';
import type { PublishCouponResponse } from '../../port/dtos/PublishCouponResponse.js';
import type { BrowseCouponsRequest } from '../../port/dtos/BrowseCouponsRequest.js';
import type { BrowseCouponsResponse } from '../../port/dtos/BrowseCouponsResponse.js';
import type { ViewProfileRequest } from '../../port/dtos/ViewProfileRequest.js';
import type { ViewProfileResponse } from '../../port/dtos/ViewProfileResponse.js';
import type { UpdateProfileRequest } from '../../port/dtos/UpdateProfileRequest.js';
import type { MyShopDriver } from '../../port/my-shop-driver.js';
import { TestUsers, testCustomer, type TestUser } from '../shared/client/http/test-user.js';
import { AUTHENTICATION_REQUIRED, MyShopUiClient, PERMISSION_DENIED } from './client/MyShopUiClient.js';
import { NewOrderPage } from './client/pages/NewOrderPage.js';
import type { CouponManagementPage } from './client/pages/CouponManagementPage.js';
import type { OrderDetailsPage } from './client/pages/OrderDetailsPage.js';

export class MyShopUiDriver implements MyShopDriver {
  private readonly client: MyShopUiClient;
  private requestedUser: TestUser | undefined;
  private anonymous = false;

  constructor(baseUrl: string, browser: Browser, keycloakBaseUrl?: string) {
    this.client = new MyShopUiClient(baseUrl, browser, keycloakBaseUrl);
  }

  actAs(identity: UserIdentity): void {
    this.anonymous = identity.kind === 'ANONYMOUS';
    switch (identity.kind) {
      case 'DEFAULT':
      case 'ANONYMOUS':
        this.requestedUser = undefined;
        break;
      case 'CUSTOMER':
        this.requestedUser = testCustomer(identity.customerIndex);
        break;
      case 'ADMIN':
        this.requestedUser = TestUsers.ADMIN;
        break;
    }
  }

  async goToMyShop(_request: GoToMyShopRequest): Promise<Result<GoToMyShopResponse, SystemError>> {
    const result = await this.client.openHomePage();
    if (result.success) return success({});
    return failure(result.error);
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    requireNotSet('Idempotency-Key', request.idempotencyKey);
    if (request.shippingAddress !== undefined && request.shippingAddress !== null) {
      throw notSupportedByUiYet('Placing an order with a shipping address');
    }
    if (this.anonymous) return this.loginRequired('/new-order');

    await this.runAs(TestUsers.CUSTOMER);
    const pageResult = await this.openNewOrderPage();
    if (!pageResult.success) return failure(pageResult.error);

    const newOrderPage = pageResult.value;
    if (request.sku !== null) {
      await newOrderPage.inputSku(request.sku);
    }
    if (request.quantity !== null) {
      await newOrderPage.inputQuantity(request.quantity);
    }
    if (request.country !== undefined && request.country !== null) {
      await newOrderPage.inputCountry(request.country);
    }
    if (request.couponCode) {
      await newOrderPage.inputCouponCode(request.couponCode);
    }
    await newOrderPage.clickPlaceOrder();

    const notificationResult = await newOrderPage.getResult();
    if (notificationResult.success) {
      const orderNumber = NewOrderPage.getOrderNumber(notificationResult.value);
      if (orderNumber) {
        return success({ orderNumber });
      }
      return failure({ message: 'Could not extract order number from success message', fieldErrors: [] });
    }
    return failure(notificationResult.error);
  }

  async viewOrder(request: ViewOrderRequest): Promise<Result<ViewOrderResponse, SystemError>> {
    if (this.anonymous) {
      return this.loginRequired(`/order-details?orderNumber=${encodeURIComponent(request.orderNumber)}`);
    }

    await this.runAs(TestUsers.CUSTOMER);
    const orderNumber = request.orderNumber;
    const detailsResult = await this.openOrderDetails(orderNumber);
    if (!detailsResult.success) return failure(detailsResult.error);
    const detailsPage = detailsResult.value;

    return success({
      orderNumber: await detailsPage.getOrderNumber(),
      orderTimestamp: await detailsPage.getOrderTimestamp(),
      sku: await detailsPage.getSku(),
      quantity: await detailsPage.getQuantity(),
      unitPrice: await detailsPage.getUnitPrice(),
      basePrice: await detailsPage.getBasePrice(),
      discountRate: await detailsPage.getDiscountRate(),
      discountAmount: await detailsPage.getDiscountAmount(),
      subtotalPrice: await detailsPage.getSubtotalPrice(),
      taxRate: await detailsPage.getTaxRate(),
      taxAmount: await detailsPage.getTaxAmount(),
      totalPrice: await detailsPage.getTotalPrice(),
      country: await detailsPage.getCountry(),
      appliedCouponCode: await detailsPage.getAppliedCouponCode(),
      status: await detailsPage.getStatus(),
    });
  }

  async cancelOrder(request: CancelOrderRequest): Promise<Result<CancelOrderResponse, SystemError>> {
    if (this.anonymous) {
      return this.loginRequired(`/order-details?orderNumber=${encodeURIComponent(request.orderNumber)}`);
    }

    await this.runAs(TestUsers.CUSTOMER);
    const orderNumber = request.orderNumber;
    const detailsResult = await this.openOrderDetails(orderNumber);
    if (!detailsResult.success) return failure(detailsResult.error);
    const detailsPage = detailsResult.value;
    await detailsPage.clickCancelOrder();

    const notificationResult = await detailsPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async deliverOrder(request: DeliverOrderRequest): Promise<Result<DeliverOrderResponse, SystemError>> {
    if (this.anonymous) {
      return this.loginRequired(`/order-details?orderNumber=${encodeURIComponent(request.orderNumber)}`);
    }

    await this.runAs(TestUsers.ADMIN);
    const orderNumber = request.orderNumber;
    const detailsResult = await this.openOrderDetails(orderNumber);
    if (!detailsResult.success) return failure(detailsResult.error);
    const detailsPage = detailsResult.value;
    if ((await detailsPage.isDeliverButtonHidden()) && (await detailsPage.getStatus()) === 'PLACED') {
      return failure({ message: PERMISSION_DENIED, fieldErrors: [] });
    }

    await detailsPage.clickDeliverOrder();

    const notificationResult = await detailsPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async browseOrderHistory(request: BrowseOrderHistoryRequest): Promise<Result<BrowseOrderHistoryResponse, SystemError>> {
    requireNotSet('page', request.page);
    requireNotSet('size', request.size);
    if (request.customer !== undefined && request.customer !== null) {
      throw notSupportedByUiYet('The order history customer filter');
    }
    if (this.anonymous) return this.loginRequired('/order-history');

    await this.runAs(TestUsers.CUSTOMER);
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);
    await homeResult.value.clickOrderHistory();

    const orderHistoryPage = this.client.orderHistoryPage();
    if (request.orderNumber) {
      await orderHistoryPage.inputOrderNumber(request.orderNumber);
      await orderHistoryPage.clickSearch();
    }

    const orderNumbers = await orderHistoryPage.readOrderNumbers();
    const orders = orderNumbers.map((orderNumber) => ({ orderNumber } as BrowseOrderHistoryOrderItem));
    return success({ orders } as BrowseOrderHistoryResponse);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<PublishCouponResponse, SystemError>> {
    if (this.anonymous) return this.loginRequired('/admin-coupons');

    await this.runAs(TestUsers.ADMIN);
    const pageResult = await this.openCouponManagementPage();
    if (!pageResult.success) return failure(pageResult.error);

    const couponPage = pageResult.value;
    await couponPage.inputCouponCode(request.code);
    await couponPage.inputDiscountRate(request.discountRate);
    if (request.validFrom) {
      await couponPage.inputValidFrom(request.validFrom);
    }
    if (request.validTo) {
      await couponPage.inputValidTo(request.validTo);
    }
    if (request.usageLimit !== undefined && request.usageLimit !== null) {
      await couponPage.inputUsageLimit(Number(request.usageLimit));
    }
    await couponPage.clickPublishCoupon();

    const notificationResult = await couponPage.getResult();
    if (notificationResult.success) return success({});
    return failure(notificationResult.error);
  }

  async browseCoupons(request: BrowseCouponsRequest): Promise<Result<BrowseCouponsResponse, SystemError>> {
    requireNotSet('page', request.page);
    requireNotSet('size', request.size);
    if (this.anonymous) return this.loginRequired('/admin-coupons');

    await this.runAs(TestUsers.ADMIN);
    const pageResult = await this.openCouponManagementPage();
    if (!pageResult.success) return failure(pageResult.error);

    const couponPage = pageResult.value;
    await couponPage.clickRefreshCouponList();
    const rows = await couponPage.getCouponRows();

    return success({ coupons: rows } as BrowseCouponsResponse);
  }

  viewProfile(_request: ViewProfileRequest): Promise<Result<ViewProfileResponse, SystemError>> {
    return Promise.reject(notSupportedByUiYet('Viewing the profile'));
  }

  updateProfile(_request: UpdateProfileRequest): Promise<Result<ViewProfileResponse, SystemError>> {
    return Promise.reject(notSupportedByUiYet('Updating the profile'));
  }

  async close(): Promise<void> {
    await this.client.close();
  }

  private async loginRequired<T>(path: string): Promise<Result<T, SystemError>> {
    await this.client.assertLoginRequiredFor(path);
    return failure({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
  }

  /** A user who has no link to the page opens its URL, which is what can be refused. */
  private async openNewOrderPage(): Promise<Result<NewOrderPage, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);

    if (this.requestedUser === TestUsers.ADMIN) return this.client.openNewOrderPage();

    await homeResult.value.clickNewOrder();
    return success(this.client.newOrderPage());
  }

  /** A user who has no link to the page opens its URL, which is what can be refused. */
  private async openCouponManagementPage(): Promise<Result<CouponManagementPage, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);

    if (this.requestedUser && this.requestedUser !== TestUsers.ADMIN) return this.client.openCouponManagementPage();

    await homeResult.value.clickAdminCoupons();
    return success(this.client.couponManagementPage());
  }

  private async openOrderDetails(orderNumber: string): Promise<Result<OrderDetailsPage, SystemError>> {
    const homeResult = await this.client.openHomePage();
    if (!homeResult.success) return failure(homeResult.error);

    const detailsPage = await this.client.openOrderDetailsPage(orderNumber);
    const loadResult = await detailsPage.getLoadResult(orderNumber);
    if (!loadResult.success) return failure(loadResult.error);
    return success(detailsPage);
  }

  /** An explicitly requested identity wins; otherwise the operation runs as its default user. */
  private async runAs(operationDefault: TestUser): Promise<void> {
    await this.client.switchUser(this.requestedUser ?? operationDefault);
  }
}

/** The UI has no screen for this yet (BUILD Steps 3-6): API-channel scenarios only. */
function notSupportedByUiYet(feature: string): Error {
  return new Error(`${feature} is not supported by the UI channel yet; run this scenario on the API channel only`);
}

/** The UI has no control for this input (no idempotency key, no paging controls): API-channel scenarios only. */
function requireNotSet(input: string, value: string | null | undefined): void {
  if (value !== null && value !== undefined) {
    throw new Error(`The UI channel cannot send '${input}'; run this scenario on the API channel only`);
  }
}
