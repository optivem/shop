using Dsl.Core.Scenario.Given;
using Dsl.Core.Scenario.When.Steps.Base;
using Driver.Adapter.Shared.Client.Http;
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
    private string? _page;
    private string? _size;
    private bool _filtersOnCustomer;
    private string? _customerAlias;
    private readonly Func<string, UserIdentity>? _resolveCustomer;

    public WhenBrowseOrderHistory(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven,
        Func<string, UserIdentity>? resolveCustomer = null)
        : base(app, scenario, ensureGiven)
    {
        _resolveCustomer = resolveCustomer;
        WithOrderNumber(DefaultOrderNumber);
    }

    public WhenBrowseOrderHistory WithOrderNumber(string? orderNumber)
    {
        _orderNumber = orderNumber;
        return this;
    }

    IWhenBrowseOrderHistory IWhenBrowseOrderHistory.WithOrderNumber(string? orderNumber) => WithOrderNumber(orderNumber);

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

    /// <summary>The username of the filtered customer; aliases resolve only once the scenario has been set up.</summary>
    private string? CustomerUsername()
    {
        if (!_filtersOnCustomer)
        {
            return null;
        }

        var identity = _customerAlias == null
            ? CustomerAliases.DefaultCustomer()
            : (_resolveCustomer ?? throw new InvalidOperationException(
                $"Customer alias '{_customerAlias}' cannot be resolved: the scenario declares no customers"))(_customerAlias);
        return TestUserExtensions.CustomerAt(identity.CustomerIndex).Username();
    }

    protected override async Task<ExecutionResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.BrowseOrderHistory()
            .OrderNumber(_orderNumber)
            .Page(_page)
            .Size(_size)
            .Customer(CustomerUsername())
            .Execute();

        return new ExecutionResultBuilder<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification>(result)
            .OrderNumber(_orderNumber)
            .Build();
    }
}
