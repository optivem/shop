using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenViewProfile : BaseWhen<ViewProfileResponse, ViewProfileVerification>, IWhenViewProfile
{
    private bool _signInWhenAsked;

    public WhenViewProfile(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenViewProfile WithSignInWhenAsked()
    {
        _signInWhenAsked = true;
        return this;
    }

    IWhenViewProfile IWhenViewProfile.WithSignInWhenAsked() => WithSignInWhenAsked();

    protected override async Task<ExecutionResult<ViewProfileResponse, ViewProfileVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var viewProfile = shop.ViewProfile();
        if (_signInWhenAsked)
        {
            viewProfile.SignInWhenAsked();
        }
        var result = await viewProfile.Execute();

        return new ExecutionResultBuilder<ViewProfileResponse, ViewProfileVerification>(result).Build();
    }
}
