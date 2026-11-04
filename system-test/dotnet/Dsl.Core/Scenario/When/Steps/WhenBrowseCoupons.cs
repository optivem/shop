using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Dsl.Core.Shared;
using Driver.Adapter;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenBrowseCoupons : BaseWhen<BrowseCouponsResponse, BrowseCouponsVerification>, IWhenBrowseCoupons
{
    private string? _page;
    private string? _size;

    public WhenBrowseCoupons(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenBrowseCoupons WithPage(string? page)
    {
        _page = page;
        return this;
    }

    IWhenBrowseCoupons IWhenBrowseCoupons.WithPage(string? page) => WithPage(page);

    public WhenBrowseCoupons WithSize(string? size)
    {
        _size = size;
        return this;
    }

    IWhenBrowseCoupons IWhenBrowseCoupons.WithSize(string? size) => WithSize(size);

    protected override async Task<ExecutionResult<BrowseCouponsResponse, BrowseCouponsVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.BrowseCoupons()
            .Page(_page)
            .Size(_size)
            .Execute();

        return new ExecutionResultBuilder<BrowseCouponsResponse, BrowseCouponsVerification>(result)
            .Build();
    }
}
