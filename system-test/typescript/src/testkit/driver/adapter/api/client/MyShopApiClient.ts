import { HealthController } from './controllers/HealthController.js';
import { OrderController } from './controllers/OrderController.js';
import { CouponController } from './controllers/CouponController.js';
import { ApiIdentity, type ApiIdentityValue } from './api-identity.js';
import { KeycloakTokenProvider } from '../../shared/client/http/keycloak-token-provider.js';
import { TestUsers, type TestUser } from '../../shared/client/http/test-user.js';
import type { BearerTokenSource } from '../../shared/client/http/bearer-token-source.js';

export class MyShopApiClient {
  private readonly healthController: HealthController;
  private readonly orderController: OrderController;
  private readonly couponController: CouponController;
  private readonly tokenProvider: KeycloakTokenProvider | undefined;
  private identity: ApiIdentityValue = ApiIdentity.DEFAULT;
  private customer: TestUser = TestUsers.CUSTOMER;

  /**
   * @param keycloakBaseUrl when undefined or empty, no token is acquired and no Authorization header is sent.
   */
  constructor(baseUrl: string, keycloakBaseUrl?: string) {
    this.tokenProvider = keycloakBaseUrl ? KeycloakTokenProvider.forBaseUrl(keycloakBaseUrl) : undefined;
    const tokenSource: BearerTokenSource | undefined = this.tokenProvider
      ? (method, path) => this.tokenFor(method, path)
      : undefined;
    this.healthController = new HealthController(baseUrl);
    this.orderController = new OrderController(baseUrl, tokenSource);
    this.couponController = new CouponController(baseUrl, tokenSource);
  }

  /** Selects who the following calls are made as. */
  as(identity: ApiIdentityValue): this {
    this.identity = identity;
    return this;
  }

  /** Calls are made as the given customer, until changed again. */
  asCustomer(customer: TestUser): this {
    this.identity = ApiIdentity.CUSTOMER;
    this.customer = customer;
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

  private async tokenFor(method: string, path: string): Promise<string | undefined> {
    const provider = this.tokenProvider;
    if (!provider) return undefined;
    switch (this.identity) {
      case ApiIdentity.ANONYMOUS:
        return undefined;
      case ApiIdentity.CUSTOMER:
        return provider.getToken(this.customer);
      case ApiIdentity.ADMIN:
        return provider.getToken(TestUsers.ADMIN);
      case ApiIdentity.DEFAULT:
        return provider.getToken(isAdminOnly(method, path) ? TestUsers.ADMIN : TestUsers.CUSTOMER);
    }
  }
}

function isAdminOnly(method: string, path: string): boolean {
  if (path.startsWith('/api/admin/')) return true;
  if (path === '/api/coupons') return true;
  return method === 'POST' && path.startsWith('/api/orders/') && path.endsWith('/deliver');
}
