using Dsl.Port;
using Dsl.Core.External.Clock;
using Driver.Port.External.Clock;
using Dsl.Core.External.Erp;
using Driver.Port.External.Erp;
using Driver.Adapter.Api;
using Driver.Adapter.Ui;
using Dsl.Core.UseCase;
using Driver.Port;
using Driver.Port.External.Tax;
using Dsl.Core.External.Tax;
using Driver.Adapter.External.Erp;
using Optivem.Testing;
using Dsl.Core.Shared;
using Driver.Adapter.External.Clock;
using Driver.Adapter.External.Tax;
using MyCompany.MyShop.SystemTest.Channel;

namespace Dsl.Core;

public class UseCaseDsl : IAsyncDisposable
{
    private const string StaticChannel = ChannelType.API;

    private readonly UseCaseContext _context;
    private readonly Configuration _configuration;
    private readonly Dictionary<string, MyShopDsl> _shops = new();
    private UserIdentity _identity = UserIdentity.Default;
    private ErpDsl? _erp;
    private TaxDsl? _tax;
    private ClockDsl? _clock;

    public UseCaseDsl(Configuration configuration)
    {
        _context = new UseCaseContext(configuration.ExternalSystemMode);
        _configuration = configuration;
    }

    /// <summary>Selects who MyShop operations are performed as, until changed again.</summary>
    public void ActAs(UserIdentity identity)
    {
        _identity = identity;
    }

    public async Task<MyShopDsl> MyShop(ChannelMode mode, Channel channel)
    {
        var channelType = ResolveMyShopChannel(mode, channel);
        return await GetOrCreateMyShop(channelType);
    }

    public async Task<MyShopDsl> MyShop(Channel channel)
    {
        return await MyShop(_configuration.ChannelMode, channel);
    }

    private async Task<MyShopDsl> GetOrCreateMyShop(string channelType)
    {
        if (!_shops.TryGetValue(channelType, out var shop))
        {
            shop = await MyShopDsl.CreateAsync(await CreateMyShopDriverForChannelAsync(channelType), _context);
            _shops[channelType] = shop;
        }
        shop.ActAs(_identity);
        return shop;
    }

    private static string ResolveMyShopChannel(ChannelMode mode, Channel channel)
    {
        var channelType = mode switch
        {
            ChannelMode.Static => StaticChannel,
            ChannelMode.Dynamic => channel.Type,
            _ => throw new InvalidOperationException($"Unknown channel mode: {mode}")
        };
        Console.WriteLine($"[ChannelMode] mode={mode} → channel={channelType}");
        return channelType;
    }

    public async Task<MyShopDsl> ApiMyShop() => await GetOrCreateMyShop(StaticChannel);

    public ErpDsl Erp() => GetOrCreate(ref _erp, () => new ErpDsl(CreateErpDriver(), _context));

    public TaxDsl Tax() => GetOrCreate(ref _tax, () => new TaxDsl(CreateTaxDriver(), _context));

    public ClockDsl Clock() => GetOrCreate(ref _clock, () => new ClockDsl(CreateClockDriver(), _context));

    private async Task<IMyShopDriver> CreateMyShopDriverForChannelAsync(string channelType)
    {
        return channelType switch
        {
            ChannelType.UI => await MyShopUiDriver.CreateAsync(_configuration.MyShopUiBaseUrl, _configuration.KeycloakBaseUrl),
            ChannelType.API => new MyShopApiDriver(_configuration.MyShopApiBaseUrl, _configuration.KeycloakBaseUrl),
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

    private ITaxDriver CreateTaxDriver()
    {
        return _context.ExternalSystemMode switch
        {
            ExternalSystemMode.Real => new TaxRealDriver(_configuration.TaxBaseUrl),
            ExternalSystemMode.Stub => new TaxStubDriver(_configuration.TaxBaseUrl),
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

        _tax?.Dispose();

        if (_clock != null)
            await _clock.DisposeAsync();

        ChannelContext.Clear();

        GC.SuppressFinalize(this);
    }

    private static T GetOrCreate<T>(ref T? instance, Func<T> supplier) where T : class
    {
        return instance ??= supplier();
    }
}
