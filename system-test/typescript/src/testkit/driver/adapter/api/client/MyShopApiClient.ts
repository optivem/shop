import { HealthController } from './controllers/HealthController.js';
import { OrderController } from './controllers/OrderController.js';
import { CouponController } from './controllers/CouponController.js';
import { ProfileController } from './controllers/ProfileController.js';
import { ProductController } from './controllers/ProductController.js';
import { AdminController } from './controllers/AdminController.js';
import { ApiIdentity, type ApiIdentityValue } from './api-identity.js';
import { KeycloakTokenProvider } from '../../shared/client/http/keycloak-token-provider.js';
import { TestUsers, type TestUser } from '../../shared/client/http/test-user.js';
import type { BearerTokenSource } from '../../shared/client/http/bearer-token-source.js';

export class MyShopApiClient {
  private readonly healthController: HealthController;
  private readonly orderController: OrderController;
  private readonly couponController: CouponController;
  private readonly profileController: ProfileController;
  private readonly productController: ProductController;
  private readonly adminController: AdminController;
  private readonly tokenProvider: KeycloakTokenProvider | undefined;
  /** Who calls are made as; undefined for no credentials. */
  private user: TestUser | undefined = TestUsers.CUSTOMER;

  /**
   * @param keycloakBaseUrl when undefined or empty, no token is acquired and no Authorization header is sent.
   */
  constructor(baseUrl: string, keycloakBaseUrl?: string) {
    this.tokenProvider = keycloakBaseUrl ? KeycloakTokenProvider.forBaseUrl(keycloakBaseUrl) : undefined;
    const tokenSource: BearerTokenSource | undefined = this.tokenProvider
      ? () => this.tokenFor()
      : undefined;
    this.healthController = new HealthController(baseUrl);
    this.orderController = new OrderController(baseUrl, tokenSource);
    this.couponController = new CouponController(baseUrl, tokenSource);
    this.profileController = new ProfileController(baseUrl, tokenSource);
    this.productController = new ProductController(baseUrl, tokenSource);
    this.adminController = new AdminController(baseUrl, tokenSource);
  }

  /** Selects who the following calls are made as. */
  as(identity: ApiIdentityValue): this {
    this.user = identity === ApiIdentity.ANONYMOUS ? undefined : TestUsers.CUSTOMER;
    return this;
  }

  /** Calls are made as the given user (a customer or an admin), until changed again. */
  asUser(user: TestUser): this {
    this.user = user;
    return this;
  }

  health(): HealthController {
    return this.healthController;
  }

  orders(): OrderController {
    return this.orderController;
  }

  coupons(): CouponController {
    return this.couponController;
  }

  profile(): ProfileController {
    return this.profileController;
  }

  products(): ProductController {
    return this.productController;
  }

  admin(): AdminController {
    return this.adminController;
  }

  private async tokenFor(): Promise<string | undefined> {
    const provider = this.tokenProvider;
    const user = this.user;
    if (!provider || !user) return undefined;
    return provider.getToken(user);
  }
}
