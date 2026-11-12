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
import {
  BRAND_SEPARATOR,
  CLEAR_FILTERS,
  REOPEN_PAGE_ADDRESS,
  type BrowseCatalogRequest,
} from '../../port/dtos/BrowseCatalogRequest.js';
import type { BrowseCatalogResponse } from '../../port/dtos/BrowseCatalogResponse.js';
import type { CatalogPage } from './client/pages/CatalogPage.js';
import type { ProductPage } from './client/pages/ProductPage.js';
import { BACK_TO_CATALOG, DECREASE, INCREASE, type ViewProductRequest } from '../../port/dtos/ViewProductRequest.js';
import type { ViewProductResponse } from '../../port/dtos/ViewProductResponse.js';
import {
  ADD_FROM_CATALOG,
  ADD_FROM_PRODUCT_PAGE,
  BROWSE_CATALOG,
  CHANGE_QUANTITY,
  DECREASE_QUANTITY,
  INCREASE_QUANTITY,
  REMOVE_LINE,
  REVISIT,
  SIGN_IN,
  VIEW_CART,
  type ManageCartRequest,
  type ManageCartRequestLineDto,
} from '../../port/dtos/ManageCartRequest.js';
import type { ManageCartResponse } from '../../port/dtos/ManageCartResponse.js';
import type { CartPage } from './client/pages/CartPage.js';
import { OPEN, PROCEED_FROM_CART, type CheckOutRequest } from '../../port/dtos/CheckOutRequest.js';
import { CART, CHECKOUT, CONFIRMATION, type CheckOutResponse } from '../../port/dtos/CheckOutResponse.js';
import { TestUsers, testUserFor, type TestUser } from '../shared/client/http/test-user.js';
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
        this.requestedUser = testUserFor(identity);
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
      return this.loginRequiredForOrder(request.orderNumber);
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
      return this.loginRequiredForOrder(request.orderNumber);
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
      return this.loginRequiredForOrder(request.orderNumber);
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

  /** The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested. */
  async browseCatalog(request: BrowseCatalogRequest): Promise<Result<BrowseCatalogResponse, SystemError>> {
    let catalogPage: CatalogPage;
    if (this.anonymous || this.requestedUser === undefined) {
      const anonymousCatalogPage = await this.client.openCatalogAnonymously();
      if (!anonymousCatalogPage) return failure({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
      catalogPage = anonymousCatalogPage;
    } else {
      await this.runAs(TestUsers.CUSTOMER);
      const pageResult = await this.client.openCatalog();
      if (!pageResult.success) return failure(pageResult.error);
      catalogPage = pageResult.value;
    }
    await catalogPage.waitForLoaded();

    if (isSet(request.headerCategory)) await catalogPage.clickHeaderCategory(request.headerCategory);
    if (isSet(request.search)) await catalogPage.search(request.search);
    if (isSet(request.category)) await catalogPage.selectCategory(request.category);
    if (isSet(request.brands)) {
      const brands = request.brands
        .split(BRAND_SEPARATOR)
        .map((brand) => brand.trim())
        .filter((brand) => brand !== '');
      for (const brand of brands) await catalogPage.tickBrand(brand);
    }
    if (isSet(request.sort)) await catalogPage.sortBy(request.sort);
    if (request.followUp === REOPEN_PAGE_ADDRESS) {
      await catalogPage.reopenPageAddress();
    } else if (request.followUp === CLEAR_FILTERS) {
      await catalogPage.clickClearFilters();
    } else if (isSet(request.followUp)) {
      throw new Error(`Unknown catalog follow-up: ${request.followUp}`);
    }

    return success(await catalogPage.read());
  }

  /**
   * Product pages are public: a visitor views them without signing in, unless a user was explicitly requested.
   * The visitor's cart line, if any, is added first, from that product's page, in the same session.
   */
  async viewProduct(request: ViewProductRequest): Promise<Result<ViewProductResponse, SystemError>> {
    if (this.anonymous || this.requestedUser === undefined) {
      await this.client.startVisitorSession();
    } else {
      await this.runAs(TestUsers.CUSTOMER);
    }
    const loginRequired = failure<ViewProductResponse, SystemError>({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });

    if (isSet(request.cartSku)) {
      const cartPageResult = await this.client.openProductPage(request.cartSku);
      if (!cartPageResult.success) return failure(cartPageResult.error);
      if (!cartPageResult.value) return loginRequired;
      await cartPageResult.value.waitForLoaded();
      await cartPageResult.value.inputQuantity(request.cartQuantity ?? '1');
      await cartPageResult.value.clickAddToCart();
    }

    let productPage: ProductPage;
    if (request.fromCatalog === true) {
      const catalogResult = await this.client.openCatalogInSession();
      if (!catalogResult.success) return failure(catalogResult.error);
      if (!catalogResult.value) return loginRequired;
      await catalogResult.value.waitForLoaded();
      productPage = await catalogResult.value.clickProduct(request.sku);
    } else {
      const pageResult = await this.client.openProductPage(request.sku);
      if (!pageResult.success) return failure(pageResult.error);
      if (!pageResult.value) return loginRequired;
      productPage = pageResult.value;
    }
    await productPage.waitForLoaded();

    if (isSet(request.quantity)) await productPage.inputQuantity(request.quantity);
    if (request.quantityStep === INCREASE) {
      await productPage.clickIncreaseQuantity();
    } else if (request.quantityStep === DECREASE) {
      await productPage.clickDecreaseQuantity();
    } else if (isSet(request.quantityStep)) {
      throw new Error(`Unknown quantity step: ${request.quantityStep}`);
    }
    const addToCart = request.addToCart === true;
    if (addToCart) await productPage.clickAddToCart();

    const response = await productPage.read();

    if (request.followUp === BACK_TO_CATALOG) {
      const catalog = await (await productPage.clickBackToCatalog()).read();
      response.catalogSkus = catalog.products.map((product) => product.sku);
    } else if (isSet(request.followUp)) {
      throw new Error(`Unknown product follow-up: ${request.followUp}`);
    }
    if (addToCart) {
      response.cartLines = await (await this.client.openCartPage()).readLines();
    }

    return success(response);
  }

  /**
   * The cart is public: a visitor keeps one without signing in, unless a user was explicitly requested. A new
   * session first gets the cart lines, each from its product page; then the action runs, and the cart is read.
   */
  async manageCart(request: ManageCartRequest): Promise<Result<ManageCartResponse, SystemError>> {
    const loginRequired = failure<ManageCartResponse, SystemError>({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
    if (request.continueSession !== true) {
      const filled = await this.startCartSession(request.cartLines ?? []);
      if (!filled.success) return failure(filled.error);
    }
    const action = request.action;
    if (!isSet(action)) return success({});

    let message: string | null = null;
    let cartItemCount: number | null = null;
    let cartPage: CartPage | null = null;
    switch (action) {
      case VIEW_CART:
        // The cart is read below.
        break;
      case BROWSE_CATALOG:
      case ADD_FROM_CATALOG: {
        const catalogResult = await this.client.openCatalogInSession();
        if (!catalogResult.success) return failure(catalogResult.error);
        if (!catalogResult.value) return loginRequired;
        const catalogPage = catalogResult.value;
        await catalogPage.waitForLoaded();
        if (action === ADD_FROM_CATALOG) message = await catalogPage.clickAddToCart(requireSku(request));
        cartItemCount = await catalogPage.readCartItemCount();
        break;
      }
      case ADD_FROM_PRODUCT_PAGE: {
        const pageResult = await this.client.openProductPage(requireSku(request));
        if (!pageResult.success) return failure(pageResult.error);
        if (!pageResult.value) return loginRequired;
        const productPage = pageResult.value;
        await productPage.waitForLoaded();
        await productPage.inputQuantity(request.quantity ?? '');
        await productPage.clickAddToCart();
        const shown = await productPage.read();
        message = shown.message ?? null;
        cartItemCount = shown.cartItemCount ?? null;
        break;
      }
      case CHANGE_QUANTITY:
      case INCREASE_QUANTITY:
      case DECREASE_QUANTITY:
      case REMOVE_LINE: {
        const sku = requireSku(request);
        cartPage = await this.client.openCartPage();
        await cartPage.waitForLoaded();
        if (action === CHANGE_QUANTITY) {
          await cartPage.inputQuantity(sku, request.quantity ?? '');
        } else if (action === INCREASE_QUANTITY) {
          await cartPage.clickIncreaseQuantity(sku);
        } else if (action === DECREASE_QUANTITY) {
          await cartPage.clickDecreaseQuantity(sku);
        } else {
          await cartPage.clickRemove(sku);
        }
        message = await cartPage.readNotification();
        break;
      }
      case REVISIT:
      case SIGN_IN: {
        if (action === REVISIT) await this.client.revisit();
        const catalogResult = await this.client.openCatalogInSession();
        if (!catalogResult.success) return failure(catalogResult.error);
        if (!catalogResult.value) return loginRequired;
        await catalogResult.value.waitForLoaded();
        if (action === SIGN_IN) await this.client.signIn(TestUsers.CUSTOMER);
        cartItemCount = await this.client.currentPageInSession().readCartItemCount();
        break;
      }
      default:
        throw new Error(`Unknown cart action: ${action}`);
    }

    cartPage ??= await this.client.openCartPage();
    const response = await cartPage.read(message, cartItemCount);
    response.askedToSignIn = this.client.isOnLoginPage();
    return success(response);
  }

  /**
   * Checkout is for signed-in customers: a visitor is asked to sign in, an admin is refused. A new session first
   * gets the cart lines, each from its product page; then the checkout is opened, and, when asked, the order placed.
   */
  async checkOut(request: CheckOutRequest): Promise<Result<CheckOutResponse, SystemError>> {
    if (request.continueSession !== true) {
      const filled = await this.startCartSession(request.cartLines ?? []);
      if (!filled.success) return failure(filled.error);
    }

    if (request.entry === PROCEED_FROM_CART) {
      await this.client.proceedToCheckoutFromCart();
    } else if (!isSet(request.entry) || request.entry === OPEN) {
      const opened = await this.client.openCheckoutPage();
      if (!opened.success) return failure(opened.error);
    } else {
      throw new Error(`Unknown checkout entry: ${request.entry}`);
    }

    if (this.client.isOnLoginPage()) {
      if (request.signIn !== true) return failure({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
      await this.client.signInOnLoginForm(TestUsers.CUSTOMER);
    }
    if (await this.client.isRefused()) {
      return failure({ message: PERMISSION_DENIED, fieldErrors: [] });
    }

    const checkoutPage = this.client.checkoutPage();
    const response: CheckOutResponse = { messageDetails: [] };
    if (!(await checkoutPage.isShown())) {
      return success(await this.readWhereTheCheckoutSentTheCustomer(response));
    }

    response.openedTaxCountry = await checkoutPage.readTaxCountry();
    response.openedEstimatedTaxRate = await checkoutPage.readEstimatedTaxRate();
    if (request.shippingAddress) {
      await checkoutPage.inputShippingAddress(request.shippingAddress);
    }
    if (isSet(request.couponCode)) {
      await checkoutPage.inputCouponCode(request.couponCode);
    }
    if (request.placeOrder !== true) {
      response.shownPage = CHECKOUT;
      await checkoutPage.readInto(response);
      return success(response);
    }

    await checkoutPage.clickPlaceOrder();
    const confirmationPage = this.client.checkoutConfirmationPage();
    if (await confirmationPage.isShown()) {
      response.shownPage = CONFIRMATION;
      await confirmationPage.readInto(response);
    } else {
      response.shownPage = CHECKOUT;
      await checkoutPage.readInto(response);
      response.confirmedOrders = [];
    }
    response.cartLines = await (await this.client.openCartPage()).readLines();
    return success(response);
  }

  async close(): Promise<void> {
    await this.client.close();
  }

  /** The checkout was not shown: the app sent the customer to the cart, or to some other page. */
  private async readWhereTheCheckoutSentTheCustomer(response: CheckOutResponse): Promise<CheckOutResponse> {
    const path = this.client.currentPath();
    if (path.endsWith(CART_PATH_SUFFIX)) {
      const cart = await this.client.cartPageInSession().read(null, null);
      response.shownPage = CART;
      response.message = cart.message ?? null;
    } else {
      response.shownPage = path;
    }
    return response;
  }

  /** A fresh session (the visitor's, or the requested user's) whose cart gets these lines, in order. */
  private async startCartSession(cartLines: ManageCartRequestLineDto[]): Promise<Result<void, SystemError>> {
    if (this.anonymous || this.requestedUser === undefined) {
      await this.client.startVisitorSession();
    } else {
      await this.runAs(TestUsers.CUSTOMER);
    }
    for (const line of cartLines) {
      const pageResult = await this.client.openProductPage(line.sku);
      if (!pageResult.success) return failure(pageResult.error);
      if (!pageResult.value) return failure({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
      await pageResult.value.waitForLoaded();
      await pageResult.value.inputQuantity(line.quantity);
      await pageResult.value.clickAddToCart();
    }
    return success(undefined);
  }

  private async loginRequired<T>(path: string): Promise<Result<T, SystemError>> {
    await this.client.assertLoginRequiredFor(path);
    return failure({ message: AUTHENTICATION_REQUIRED, fieldErrors: [] });
  }

  private async loginRequiredForOrder<T>(orderNumber: string): Promise<Result<T, SystemError>> {
    const encodedOrderNumber = encodeURIComponent(orderNumber);
    await this.client.assertLoginRequiredFor(`/order-details?orderNumber=${encodedOrderNumber}`, `/order-details/${encodedOrderNumber}`);
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

const CART_PATH_SUFFIX = '/cart';

function isSet(value: string | null | undefined): value is string {
  return value !== null && value !== undefined;
}

function requireSku(request: ManageCartRequest): string {
  if (!isSet(request.sku)) throw new Error(`The cart action '${request.action}' needs a SKU`);
  return request.sku;
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
