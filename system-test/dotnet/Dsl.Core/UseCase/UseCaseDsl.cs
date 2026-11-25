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
using Driver.Adapter.External.Identity;
using Driver.Port.External.Identity;
using MyCompany.MyShop.SystemTest.Channel;

namespace Dsl.Core;

public class UseCaseDsl : IAsyncDisposable
{
    private const string StaticChannel = ChannelType.API;
    private const string AdminFirstName = "Test";
    private const string AdminLastName = "Admin";

    private readonly UseCaseContext _context;
    private readonly Configuration _configuration;
    private readonly Dictionary<string, MyShopDsl> _shops = new();
    private UserIdentity _identity = UserIdentity.Default;
    private ErpDsl? _erp;
    private TaxDsl? _tax;
    private ClockDsl? _clock;
    private IIdentityProviderDriver? _identityProvider;
    private UserIdentity? _admin;

    public UseCaseDsl(Configuration configuration)
    {
        _context = new UseCaseContext(configuration.ExternalSystemMode);
        _configuration = configuration;
    }

    /// <summary>
    /// Creates a customer account of this scenario's own (customer role, no saved profile, no orders), removed again
    /// when this DSL is disposed.
    /// </summary>
    /// <param name="email">null for a unique address</param>
    public async Task<UserIdentity> NewCustomerAsync(string firstName, string lastName, string? email)
    {
        _identityProvider ??= new KeycloakIdentityProviderDriver(_configuration.KeycloakBaseUrl);
        return UserIdentity.Customer(await _identityProvider.CreateCustomerAsync(firstName, lastName, email));
    }

    /// <summary>
    /// A visitor signs up through the shop (UC-013, the UI channel only): the identity provider's registration form
    /// creates the account, with this name and shipping country. It is removed again when this DSL is disposed.
    /// </summary>
    /// <param name="email">null for a unique address</param>
    public async Task<UserIdentity> SignedUpCustomerAsync(string firstName, string lastName, string? email,
        string country, Channel channel)
    {
        _identityProvider ??= new KeycloakIdentityProviderDriver(_configuration.KeycloakBaseUrl);
        var account = _identityProvider.NewAccountCredentials();
        try
        {
            var shop = await MyShop(ChannelMode.Dynamic, channel);
            (await shop.SignUp()
                .Account(account.Username, account.Password)
                .Email(email ?? _identityProvider.NewAccountEmail(account))
                .Name(firstName, lastName)
                .Country(country)
                .Execute())
                .ShouldSucceed();
        }
        finally
        {
            await RemoveOnDisposeAsync(account);
        }
        return UserIdentity.Customer(account);
    }

    /// <summary>Also when the form was submitted but the shop never showed the customer signed in; no account, nothing to do.</summary>
    private async Task RemoveOnDisposeAsync(UserAccount account)
    {
        try
        {
            await _identityProvider!.DeleteOnDisposeAsync(account.Username);
        }
        catch (InvalidOperationException e)
        {
            Console.WriteLine($"No account {account.Username} to remove after the sign-up: {e.Message}");
        }
    }

    /// <summary>Creates an admin account of this scenario's own (admin role), removed again when this DSL is disposed.</summary>
    /// <param name="email">null for a unique address</param>
    public async Task<UserIdentity> NewAdminAsync(string firstName, string lastName, string? email)
    {
        _identityProvider ??= new KeycloakIdentityProviderDriver(_configuration.KeycloakBaseUrl);
        return UserIdentity.Admin(await _identityProvider.CreateAdminAsync(firstName, lastName, email));
    }

    /// <summary>
    /// This scenario's admin, created the first time it is needed: who admin-only operations run as, unless another
    /// identity is selected.
    /// </summary>
    public async Task<UserIdentity> AdminAsync()
    {
        _admin ??= await NewAdminAsync(AdminFirstName, AdminLastName, null);
        return _admin.Value;
    }

    /// <summary>Selects who MyShop operations are performed as, until changed again.</summary>
    public void ActAs(UserIdentity identity)
    {
        _identity = identity;
    }

    /// <summary>Runs <paramref name="action"/> as <paramref name="identity"/>, then acts as before again.</summary>
    public async Task<T> ActingAsAsync<T>(UserIdentity identity, Func<Task<T>> action)
    {
        var previous = _identity;
        ActAs(identity);
        try
        {
            return await action();
        }
        finally
        {
            ActAs(previous);
        }
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
            var driver = await CreateMyShopDriverForChannelAsync(channelType);
            driver.UseScenarioAdmin(AdminAsync);
            shop = await MyShopDsl.CreateAsync(driver, _context);
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

        if (_tax != null)
            await _tax.DisposeAsync();

        if (_clock != null)
            await _clock.DisposeAsync();

        // Last: the shops' browser sessions may still be signed in as a created customer.
        if (_identityProvider != null)
            await _identityProvider.DisposeAsync();

        ChannelContext.Clear();

        GC.SuppressFinalize(this);
    }

    private static T GetOrCreate<T>(ref T? instance, Func<T> supplier) where T : class
    {
        return instance ??= supplier();
    }
}
