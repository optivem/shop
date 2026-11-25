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
import type { BrowseOrderHistoryResponse } from '../../port/dtos/BrowseOrderHistoryResponse.js';
import type { PublishCouponRequest } from '../../port/dtos/PublishCouponRequest.js';
import type { PublishCouponResponse } from '../../port/dtos/PublishCouponResponse.js';
import type { BrowseCouponsRequest } from '../../port/dtos/BrowseCouponsRequest.js';
import type { BrowseCouponsResponse } from '../../port/dtos/BrowseCouponsResponse.js';
import type { BrowseCustomersRequest } from '../../port/dtos/BrowseCustomersRequest.js';
import type { BrowseCustomersResponse } from '../../port/dtos/BrowseCustomersResponse.js';
import type { ViewProfileRequest } from '../../port/dtos/ViewProfileRequest.js';
import type { ViewProfileResponse } from '../../port/dtos/ViewProfileResponse.js';
import type { UpdateProfileRequest } from '../../port/dtos/UpdateProfileRequest.js';
import type { SignUpRequest } from '../../port/dtos/SignUpRequest.js';
import type { SignUpResponse } from '../../port/dtos/SignUpResponse.js';
import type { BrowseCatalogRequest } from '../../port/dtos/BrowseCatalogRequest.js';
import type { BrowseCatalogResponse } from '../../port/dtos/BrowseCatalogResponse.js';
import type { ViewProductRequest } from '../../port/dtos/ViewProductRequest.js';
import type { ViewProductResponse } from '../../port/dtos/ViewProductResponse.js';
import type { ManageCartRequest } from '../../port/dtos/ManageCartRequest.js';
import type { ManageCartResponse } from '../../port/dtos/ManageCartResponse.js';
import type { CheckOutRequest } from '../../port/dtos/CheckOutRequest.js';
import type { CheckOutResponse } from '../../port/dtos/CheckOutResponse.js';
import type { CheckAccessibilityRequest } from '../../port/dtos/CheckAccessibilityRequest.js';
import type { CheckAccessibilityResponse } from '../../port/dtos/CheckAccessibilityResponse.js';
import type { MyShopDriver } from '../../port/my-shop-driver.js';
import type { UserIdentity } from '../../port/user-identity.js';
import { TestUsers, testUserOf, type TestUser } from '../shared/client/http/test-user.js';
import { MyShopApiClient } from './client/MyShopApiClient.js';
import { ApiIdentity } from './client/api-identity.js';

export class MyShopApiDriver implements MyShopDriver {
  private readonly client: MyShopApiClient;
  private readonly signsIn: boolean;
  private requestedUser: TestUser | undefined;
  private anonymous = false;
  private scenarioAdmin: () => Promise<UserIdentity> = () =>
    Promise.reject(
      new Error(
        "No admin for this admin-only operation: act as an admin, or give the driver the scenario's admin (useScenarioAdmin)",
      ),
    );

  constructor(baseUrl: string, keycloakBaseUrl?: string) {
    this.client = new MyShopApiClient(baseUrl, keycloakBaseUrl);
    this.signsIn = !!keycloakBaseUrl;
  }

  actAs(identity: UserIdentity): void {
    this.anonymous = identity.kind === 'ANONYMOUS';
    switch (identity.kind) {
      case 'DEFAULT':
      case 'ANONYMOUS':
        this.requestedUser = undefined;
        break;
      case 'CUSTOMER':
      case 'ADMIN':
        this.requestedUser = testUserOf(identity);
        break;
    }
  }

  useScenarioAdmin(admin: () => Promise<UserIdentity>): void {
    this.scenarioAdmin = admin;
  }

  /** An explicitly requested identity wins; otherwise the operation runs as its default user. */
  private runAs(operationDefault: TestUser): MyShopApiClient {
    const user = this.requestedUser ?? operationDefault;
    if (this.anonymous) return this.client.as(ApiIdentity.ANONYMOUS);
    return this.client.asUser(user);
  }

  /**
   * Runs an admin-only operation: as the requested identity, else as the scenario's admin. Without sign-in (no
   * Keycloak) no token is sent anyway, so no admin is created.
   */
  private async runAsAdmin(): Promise<MyShopApiClient> {
    const needsAdmin = this.requestedUser === undefined && !this.anonymous && this.signsIn;
    return this.runAs(needsAdmin ? await this.scenarioAdminUser() : TestUsers.CUSTOMER);
  }

  private async scenarioAdminUser(): Promise<TestUser> {
    const admin = await this.scenarioAdmin();
    if (admin.kind !== 'ADMIN') throw new Error(`The scenario's admin is a ${admin.kind} identity`);
    return testUserOf(admin);
  }

  async goToMyShop(_request: GoToMyShopRequest): Promise<Result<GoToMyShopResponse, SystemError>> {
    const result = await this.runAs(TestUsers.CUSTOMER).health().checkHealth();
    if (result.success) return success({});
    return failure(result.error);
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.runAs(TestUsers.CUSTOMER).orders().placeOrder(request);
  }

  async cancelOrder(request: CancelOrderRequest): Promise<Result<CancelOrderResponse, SystemError>> {
    const result = await this.runAs(TestUsers.CUSTOMER).orders().cancelOrder(request.orderNumber);
    if (result.success) return success({});
    return failure(result.error);
  }

  async deliverOrder(request: DeliverOrderRequest): Promise<Result<DeliverOrderResponse, SystemError>> {
    const result = await (await this.runAsAdmin()).orders().deliverOrder(request.orderNumber);
    if (result.success) return success({});
    return failure(result.error);
  }

  async viewOrder(request: ViewOrderRequest): Promise<Result<ViewOrderResponse, SystemError>> {
    requireNotSet('whenAskedToSignIn', request.whenAskedToSignIn);
    return this.runAs(TestUsers.CUSTOMER).orders().viewOrder(request.orderNumber);
  }

  async browseOrderHistory(request: BrowseOrderHistoryRequest): Promise<Result<BrowseOrderHistoryResponse, SystemError>> {
    requireNotSet('followUp', request.followUp);
    return this.runAs(TestUsers.CUSTOMER)
      .orders()
      .browseOrderHistory(request.orderNumber, request.page, request.size, request.customer);
  }

  async browseCustomers(_request: BrowseCustomersRequest): Promise<Result<BrowseCustomersResponse, SystemError>> {
    return (await this.runAsAdmin()).admin().browseCustomers();
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<PublishCouponResponse, SystemError>> {
    const result = await (await this.runAsAdmin()).coupons().publishCoupon(request);
    if (result.success) return success({});
    return failure(result.error);
  }

  async browseCoupons(request: BrowseCouponsRequest): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return (await this.runAsAdmin()).coupons().browseCoupons(request.page, request.size);
  }

  async viewProfile(request: ViewProfileRequest): Promise<Result<ViewProfileResponse, SystemError>> {
    requireNotSet('whenAskedToSignIn', request.whenAskedToSignIn);
    return this.runAs(TestUsers.CUSTOMER).profile().viewProfile();
  }

  async updateProfile(request: UpdateProfileRequest): Promise<Result<ViewProfileResponse, SystemError>> {
    return this.runAs(TestUsers.CUSTOMER).profile().updateProfile(request);
  }

  /** The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested. */
  async browseCatalog(request: BrowseCatalogRequest): Promise<Result<BrowseCatalogResponse, SystemError>> {
    requireNotSet('headerCategory', request.headerCategory);
    requireNotSet('search', request.search);
    requireNotSet('category', request.category);
    requireNotSet('brands', request.brands);
    requireNotSet('sort', request.sort);
    requireNotSet('followUp', request.followUp);

    const client =
      this.anonymous || this.requestedUser === undefined
        ? this.client.as(ApiIdentity.ANONYMOUS)
        : this.runAs(TestUsers.CUSTOMER);
    const result = await client.products().browseProducts();
    if (!result.success) return failure(result.error);
    return success({ products: result.value, productCount: result.value.length });
  }

  async viewProduct(request: ViewProductRequest): Promise<Result<ViewProductResponse, SystemError>> {
    requireNotSet('fromCatalog', request.fromCatalog);
    requireNotSet('cartSku', request.cartSku);
    requireNotSet('quantity', request.quantity);
    requireNotSet('quantityStep', request.quantityStep);
    requireNotSet('addToCart', request.addToCart);
    requireNotSet('followUp', request.followUp);

    const client =
      this.anonymous || this.requestedUser === undefined
        ? this.client.as(ApiIdentity.ANONYMOUS)
        : this.runAs(TestUsers.CUSTOMER);
    const result = await client.products().viewProduct(request.sku);
    if (!result.success) return failure(result.error);
    const product = result.value;
    return success({
      sku: product.sku,
      title: product.title,
      brand: product.brand,
      category: product.category,
      description: product.description,
      price: product.price,
    });
  }

  /** The cart lives in the browser (session, cookie or local storage): there is no cart API. */
  manageCart(_request: ManageCartRequest): Promise<Result<ManageCartResponse, SystemError>> {
    return Promise.reject(new Error('The API channel has no cart; run this scenario on the UI channel only'));
  }

  /** The cart lives in the browser, so there is no checkout API either. */
  checkOut(_request: CheckOutRequest): Promise<Result<CheckOutResponse, SystemError>> {
    return Promise.reject(new Error('The API channel has no cart to check out; run this scenario on the UI channel only'));
  }

  /** Signing up is the identity provider's registration form, reached from the shop's pages: there is no sign-up API. */
  signUp(_request: SignUpRequest): Promise<Result<SignUpResponse, SystemError>> {
    return Promise.reject(new Error('The API channel has no sign-up form; run this scenario on the UI channel only'));
  }

  /** The API serves data, not pages: accessibility is a UI-channel check. */
  checkAccessibility(_request: CheckAccessibilityRequest): Promise<Result<CheckAccessibilityResponse, SystemError>> {
    return Promise.reject(new Error('The API channel has no pages to check for accessibility; run this scenario on the UI channel only'));
  }

  async close(): Promise<void> {
    // No resources to release: the API client uses fetch per-call.
  }
}

/** The API serves the unfiltered catalog; filtering, search and sort happen in the UI. */
function requireNotSet(input: string, value: unknown): void {
  if (value !== undefined && value !== null) {
    throw new Error(`The API channel cannot send '${input}'; run this scenario on the UI channel only`);
  }
}
