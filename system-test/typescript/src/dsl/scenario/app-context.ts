import { ShopDriver, ErpDriver, ClockDriver, TaxDriver } from '../../drivers/types.js';

export type ChannelMode = 'dynamic' | 'static';

const STATIC_CHANNEL = 'api';

export class AppContext {
  private readonly shops = new Map<string, ShopDriver>();
  private readonly channelMode: ChannelMode;
  private readonly channel: string;
  private readonly shopDriverFactory: (channel: string) => ShopDriver;
  readonly erpDriver: ErpDriver;
  readonly clockDriver: ClockDriver;
  readonly taxDriver: TaxDriver;

  constructor(opts: {
    channelMode: ChannelMode;
    channel: string;
    shopDriverFactory: (channel: string) => ShopDriver;
    erpDriver: ErpDriver;
    clockDriver: ClockDriver;
    taxDriver: TaxDriver;
  }) {
    this.channelMode = opts.channelMode;
    this.channel = opts.channel;
    this.shopDriverFactory = opts.shopDriverFactory;
    this.erpDriver = opts.erpDriver;
    this.clockDriver = opts.clockDriver;
    this.taxDriver = opts.taxDriver;
  }

  shop(mode?: ChannelMode): ShopDriver {
    const resolvedMode = mode ?? this.channelMode;
    const channel = resolvedMode === 'static' ? STATIC_CHANNEL : this.channel;
    if (!this.shops.has(channel)) {
      this.shops.set(channel, this.shopDriverFactory(channel));
    }
    return this.shops.get(channel)!;
  }

  async closeAll(): Promise<void> {
    for (const driver of this.shops.values()) {
      await driver.close();
    }
    await this.erpDriver.close();
    await this.clockDriver.close();
    await this.taxDriver.close();
  }
}
