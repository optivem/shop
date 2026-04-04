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

namespace Dsl.Core;

public class UseCaseDsl : IAsyncDisposable
{
    private readonly UseCaseContext _context;
    private readonly Configuration _configuration;
    private ShopDsl? _shop;
    private ShopDsl? _dynamicShop;
    private ErpDsl? _erp;
    private ClockDsl? _clock;

    public UseCaseDsl(UseCaseContext context, Configuration configuration)
    {
        _context = context;
        _configuration = configuration;
    }

    public UseCaseDsl(Configuration configuration)
        : this(new UseCaseContext(configuration.ExternalSystemMode), configuration) { }

    public async Task<ShopDsl> Shop(Channel channel)
    {
        var effectiveType = _configuration.ChannelMode == ChannelMode.Static
            ? _configuration.StaticChannel!
            : channel.Type;

        if (_shop == null)
        {
            _shop = await ShopDsl.CreateAsync(await CreateShopDriverForChannelAsync(effectiveType), _context);
        }
        return _shop;
    }

    public async Task<ShopDsl> Shop(ChannelMode mode, Channel channel)
    {
        if (mode == ChannelMode.Dynamic)
        {
            if (_configuration.ChannelMode == ChannelMode.Dynamic)
            {
                return await Shop(channel);
            }

            if (_dynamicShop == null)
            {
                _dynamicShop = await ShopDsl.CreateAsync(await CreateShopDriverForChannelAsync(channel.Type), _context);
            }
            return _dynamicShop;
        }
        return await Shop(channel);
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
        if (_shop != null)
            await _shop.DisposeAsync();

        if (_dynamicShop != null && _dynamicShop != _shop)
            await _dynamicShop.DisposeAsync();

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





