using Dsl.Core.Scenario.Given;
using Dsl.Core.Scenario.When.Steps.Base;
using Driver.Port;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Adapter;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenBrowseOrderHistory : BaseWhen<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>, IWhenBrowseOrderHistory
{
    private string? _orderNumber;
    private bool _orderNumberPartOnly;
    private string? _page;
    private string? _size;
    private bool _filtersOnCustomer;
    private string? _customerAlias;
    private bool _resetFiltersAfterwards;
    private readonly Func<string, UserIdentity>? _resolveCustomer;
    private readonly Func<UserIdentity> _defaultCustomer;

    /// <param name="defaultCustomer">the scenario's default customer; customer1 when the scenario has no givens</param>
    public WhenBrowseOrderHistory(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven,
        Func<string, UserIdentity>? resolveCustomer = null, Func<UserIdentity>? defaultCustomer = null)
        : base(app, scenario, ensureGiven)
    {
        _resolveCustomer = resolveCustomer;
        _defaultCustomer = defaultCustomer ?? CustomerAliases.DefaultCustomer;
        WithOrderNumber(DefaultOrderNumber);
    }

    public WhenBrowseOrderHistory WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        _orderNumberPartOnly = false;
        return this;
    }

    public WhenBrowseOrderHistory WithOrderNumberPartOf(string orderNumber)
    {
        _orderNumber = orderNumber;
        _orderNumberPartOnly = true;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithOrderNumberPartOf(string orderNumber) => WithOrderNumberPartOf(orderNumber);

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

    public WhenBrowseOrderHistory WithNoOrderNumberFilter() => WithOrderNumber(null);

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithNoOrderNumberFilter() => WithNoOrderNumberFilter();

    public WhenBrowseOrderHistory WithFiltersResetAfterwards()
    {
        _resetFiltersAfterwards = true;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithFiltersResetAfterwards() => WithFiltersResetAfterwards();

    public WhenBrowseOrderHistory WithPage(string? page)
    {
        _page = page;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithPage(string? page) => WithPage(page);

    public WhenBrowseOrderHistory WithSize(string? size)
    {
        _size = size;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithSize(string? size) => WithSize(size);

    public WhenBrowseOrderHistory WithCustomer()
    {
        _filtersOnCustomer = true;
        _customerAlias = null;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithCustomer() => WithCustomer();

    public WhenBrowseOrderHistory WithCustomer(string customerAlias)
    {
        _filtersOnCustomer = true;
        _customerAlias = customerAlias;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithCustomer(string customerAlias) => WithCustomer(customerAlias);

    private string ResolveCustomerUsername(string customerAlias)
        => (_resolveCustomer ?? throw new InvalidOperationException(
            $"Customer alias '{customerAlias}' cannot be resolved: the scenario declares no customers"))(customerAlias).Username();

    /// <summary>The username of the filtered customer; aliases resolve only once the scenario has been set up.</summary>
    private string? CustomerUsername()
    {
        if (!_filtersOnCustomer)
        {
            return null;
        }

        var identity = _customerAlias == null
            ? _defaultCustomer()
            : (_resolveCustomer ?? throw new InvalidOperationException(
                $"Customer alias '{_customerAlias}' cannot be resolved: the scenario declares no customers"))(_customerAlias);
        return identity.Username();
    }

    protected override async Task<ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var browseOrderHistory = shop.BrowseOrderHistory()
            .OrderNumber(_orderNumber)
            .Page(_page)
            .Size(_size)
            .Customer(CustomerUsername());
        if (_orderNumberPartOnly)
        {
            browseOrderHistory.OrderNumberPartOf(_orderNumber!);
        }
        if (_resetFiltersAfterwards)
        {
            browseOrderHistory.ResetFiltersAfterwards();
        }
        var result = await browseOrderHistory.Execute();

        return new ExecutionResultBuilder<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(result)
            .OrderNumber(_orderNumber)
            .CustomerUsernames(ResolveCustomerUsername)
            .Build();
    }
}
