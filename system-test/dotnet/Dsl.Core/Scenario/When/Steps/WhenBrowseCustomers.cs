using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Adapter;
using Driver.Port;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenBrowseCustomers : BaseWhen<BrowseCustomersResponse, BrowseCustomersVerification>, IWhenBrowseCustomers
{
    private readonly Func<string, UserIdentity>? _resolveCustomer;

    public WhenBrowseCustomers(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven,
        Func<string, UserIdentity>? resolveCustomer = null)
        : base(app, scenario, ensureGiven)
    {
        _resolveCustomer = resolveCustomer;
    }

    private string ResolveCustomerUsername(string customerAlias)
        => (_resolveCustomer ?? throw new InvalidOperationException(
            $"Customer alias '{customerAlias}' cannot be resolved: the scenario declares no customers"))(customerAlias).Username();

    protected override async Task<ExecutionResult<BrowseCustomersResponse, BrowseCustomersVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.BrowseCustomers().Execute();

        return new ExecutionResultBuilder<BrowseCustomersResponse, BrowseCustomersVerification>(result)
            .CustomerUsernames(ResolveCustomerUsername)
            .Build();
    }
}
