using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenViewProfile : BaseWhen<ViewProfileResponse, ViewProfileVerification>, IWhenViewProfile
{
    public WhenViewProfile(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    protected override async Task<ExecutionResult<ViewProfileResponse, ViewProfileVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.ViewProfile().Execute();

        return new ExecutionResultBuilder<ViewProfileResponse, ViewProfileVerification>(result).Build();
    }
}
