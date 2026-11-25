import { ChannelType } from '../../../channel/channel-type.js';
import type { MyShopDriver } from '../../../driver/port/my-shop-driver.js';
import type { ErpDriver } from '../../../driver/port/external/erp/erp-driver.js';
import type { ClockDriver } from '../../../driver/port/external/clock/clock-driver.js';
import type { TaxDriver } from '../../../driver/port/external/tax/tax-driver.js';
import type { IdentityProviderDriver } from '../../../driver/port/external/identity/identity-provider-driver.js';
import { UserIdentity } from '../../../driver/port/user-identity.js';
import type { UserAccount } from '../../../driver/port/user-account.js';
import type { UseCaseContext } from '../shared/use-case-context.js';
import { MyShopDsl } from '../usecase/MyShopDsl.js';

export type ChannelMode = 'dynamic' | 'static';

const STATIC_CHANNEL = ChannelType.API;
const ADMIN_FIRST_NAME = 'Test';
const ADMIN_LAST_NAME = 'Admin';

export class AppContext {
  private readonly shops = new Map<string, MyShopDriver>();
  private identity: UserIdentity = UserIdentity.DEFAULT;
  private readonly channelMode: ChannelMode;
  private readonly channel: string;
  private readonly myShopDriverFactory: (channel: string) => MyShopDriver;
  readonly erpDriver: ErpDriver;
  readonly clockDriver: ClockDriver;
  readonly taxDriver: TaxDriver;
  private readonly identityProviderDriverFactory?: () => IdentityProviderDriver;
  private identityProviderDriver?: IdentityProviderDriver;
  private admin?: Promise<UserIdentity>;

  constructor(opts: {
    channelMode: ChannelMode;
    channel: string;
    myShopDriverFactory: (channel: string) => MyShopDriver;
    erpDriver: ErpDriver;
    clockDriver: ClockDriver;
    taxDriver: TaxDriver;
    /** Creates customers and admins of a scenario's own; without it a scenario cannot have a new customer or admin. */
    identityProviderDriverFactory?: () => IdentityProviderDriver;
  }) {
    this.channelMode = opts.channelMode;
    this.channel = opts.channel;
    this.myShopDriverFactory = opts.myShopDriverFactory;
    this.erpDriver = opts.erpDriver;
    this.clockDriver = opts.clockDriver;
    this.taxDriver = opts.taxDriver;
    this.identityProviderDriverFactory = opts.identityProviderDriverFactory;
  }

  /**
   * Creates a customer account of the scenario's own (customer role, no saved profile, no orders), removed again
   * by {@link closeAll}.
   */
  async newCustomer(firstName: string, lastName: string, email?: string): Promise<UserIdentity> {
    return UserIdentity.createdCustomer(await this.identityProvider().createCustomer(firstName, lastName, email));
  }

  /**
   * A visitor signs up through the shop (UC-013, the UI channel only): the identity provider's registration form
   * creates the account, with this name and shipping country. It is removed again by {@link closeAll}.
   * @param email undefined for a unique address
   */
  async signedUpCustomer(
    firstName: string,
    lastName: string,
    email: string | undefined,
    country: string,
    useCaseContext: UseCaseContext,
  ): Promise<UserIdentity> {
    const identityProvider = this.identityProvider();
    const account = identityProvider.newAccountCredentials();
    try {
      const result = await new MyShopDsl(this.myShop('dynamic'), useCaseContext)
        .signUp()
        .account(account.username, account.password)
        .email(email ?? identityProvider.newAccountEmail(account))
        .name(firstName, lastName)
        .country(country)
        .execute();
      result.shouldSucceed();
    } finally {
      await this.removeOnClose(identityProvider, account);
    }
    return UserIdentity.createdCustomer(account);
  }

  /** Also when the form was submitted but the shop never showed the customer signed in; no account, nothing to do. */
  private async removeOnClose(identityProvider: IdentityProviderDriver, account: UserAccount): Promise<void> {
    try {
      await identityProvider.deleteOnClose(account.username);
    } catch (e) {
      console.info(`No account ${account.username} to remove after the sign-up: ${e instanceof Error ? e.message : String(e)}`);
    }
  }

  /** Creates an admin account of the scenario's own (admin role), removed again by {@link closeAll}. */
  async newAdmin(firstName: string, lastName: string, email?: string): Promise<UserIdentity> {
    return UserIdentity.admin(await this.identityProvider().createAdmin(firstName, lastName, email));
  }

  /**
   * The scenario's admin, created the first time it is needed: who admin-only operations run as, unless another
   * identity is selected.
   */
  scenarioAdmin(): Promise<UserIdentity> {
    this.admin ??= this.newAdmin(ADMIN_FIRST_NAME, ADMIN_LAST_NAME);
    return this.admin;
  }

  private identityProvider(): IdentityProviderDriver {
    if (!this.identityProviderDriver) {
      if (!this.identityProviderDriverFactory) {
        throw new Error(
          'This app context was built without an identity provider, so it cannot create a new customer or admin',
        );
      }
      this.identityProviderDriver = this.identityProviderDriverFactory();
    }
    return this.identityProviderDriver;
  }

  /** Selects who MyShop operations are performed as, until changed again. */
  actAs(identity: UserIdentity): void {
    this.identity = identity;
  }

  /** Runs `action` as `identity`, then acts as before again. */
  async actingAs<T>(identity: UserIdentity, action: () => Promise<T>): Promise<T> {
    const previous = this.identity;
    this.actAs(identity);
    try {
      return await action();
    } finally {
      this.actAs(previous);
    }
  }

  myShop(mode?: ChannelMode): MyShopDriver {
    const resolvedMode = mode ?? this.channelMode;
    const channel = resolvedMode === 'static' ? STATIC_CHANNEL : this.channel;
    if (!this.shops.has(channel)) {
      const driver = this.myShopDriverFactory(channel);
      driver.useScenarioAdmin(() => this.scenarioAdmin());
      this.shops.set(channel, driver);
    }
    const shop = this.shops.get(channel)!;
    shop.actAs(this.identity);
    return shop;
  }

  async closeAll(): Promise<void> {
    for (const driver of this.shops.values()) {
      await driver.close();
    }
    await this.erpDriver.close();
    await this.clockDriver.close();
    await this.taxDriver.close();
    // Last: the shops' browser sessions may still be signed in as a created customer.
    await this.identityProviderDriver?.close();
  }
}
