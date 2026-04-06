using Dsl.Port;
using Dsl.Core.External.Clock;
using Driver.Port.External.Clock;
using Dsl.Core.External.Erp;
using Driver.Port.External.Erp;
using Driver.Adapter.Shop.Api;
using Driver.Adapter.Shop.Ui;
using Dsl.Core.Shop;
using Driver.Port.Shop;
using Dsl.Core.Shop;
using Driver.Adapter.External.Erp;
using Optivem.Testing;
using Dsl.Core.Shared;
using Driver.Adapter.External.Clock;
using Optivem.Shop.SystemTest.Channel;

namespace Dsl.Core;

public class UseCaseDsl : IAsyncDisposable
{
    private const string StaticChannel = ChannelType.API;

    private readonly UseCaseContext _context;
    private readonly Configuration _configuration;
    private readonly Dictionary<string, ShopDsl> _shops = new();
    private ErpDsl? _erp;
    private ClockDsl? _clock;

    public UseCaseDsl(Configuration configuration)
    {
        _context = new UseCaseContext(configuration.ExternalSystemMode);
        _configuration = configuration;
    }

    public async Task<ShopDsl> Shop(ChannelMode mode, Channel channel)
    {
        var channelType = ResolveShopChannel(mode, channel);
        return await GetOrCreateShop(channelType);
    }

    public async Task<ShopDsl> Shop(Channel channel)
    {
        return await Shop(_configuration.ChannelMode, channel);
    }

    private async Task<ShopDsl> GetOrCreateShop(string channelType)
    {
        if (!_shops.TryGetValue(channelType, out var shop))
        {
            shop = await ShopDsl.CreateAsync(await CreateShopDriverForChannelAsync(channelType), _context);
            _shops[channelType] = shop;
        }
        return shop;
    }

    private static string ResolveShopChannel(ChannelMode mode, Channel channel)
    {
        return mode switch
        {
            ChannelMode.Static => StaticChannel,
            ChannelMode.Dynamic => channel.Type,
            _ => throw new InvalidOperationException($"Unknown channel mode: {mode}")
        };
    }

    public ErpDsl Erp() => GetOrCreate(ref _erp, () => new ErpDsl(CreateErpDriver(), _context));

    public ClockDsl Clock() => GetOrCreate(ref _clock, () => new ClockDsl(CreateClockDriver(), _context));

    private async Task<IShopDriver> CreateShopDriverForChannelAsync(string channelType)
    {
        return channelType switch
        {
            ChannelType.UI => await ShopUiDriver.CreateAsync(_configuration.ShopUiBaseUrl),
            ChannelType.API => new ShopApiDriver(_configuration.ShopApiBaseUrl),
            _ => throw new InvalidOperationException($"Unknown channel type: {channelType}")
        };
    }

    private IErpDriver CreateErpDriver()
    {
        return _context.ExternalSystemMode switch
        {
            ExternalSystemMode.Real => new ErpRealDriver(_configuration.ErpBaseUrl),
            ExternalSystemMode.Stub => new ErpStubDriver(_configuration.ErpBaseUrl),
            _ => throw new InvalidOperationException($"Unknown external system mode: {_context.ExternalSystemMode}")
        };
    }

    private IClockDriver CreateClockDriver()
    {
        return _context.ExternalSystemMode switch
        {
            ExternalSystemMode.Real => new ClockRealDriver(),
            ExternalSystemMode.Stub => new ClockStubDriver(_configuration.ClockBaseUrl),
            _ => throw new InvalidOperationException($"Unknown external system mode: {_context.ExternalSystemMode}")
        };
    }

    public async ValueTask DisposeAsync()
    {
        foreach (var shop in _shops.Values)
            await shop.DisposeAsync();

        if (_erp != null)
            await _erp.DisposeAsync();

        if (_clock != null)
            await _clock.DisposeAsync();

        ChannelContext.Clear();
    }

    private static T GetOrCreate<T>(ref T? instance, Func<T> supplier) where T : class
    {
        return instance ??= supplier();
    }
}





