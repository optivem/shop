using Dsl.Port.Assume;
using Dsl.Port.Assume.Steps;
using Optivem.Testing;

namespace Dsl.Core.Scenario.Assume;

public class AssumeStage : IAssumeStage
{
    private readonly UseCaseDsl _app;
    private readonly Channel? _channel;

    public AssumeStage(UseCaseDsl app, Channel? channel = null)
    {
        _app = app;
        _channel = channel;
    }

    public IAssumeRunning MyShop()
    {
        return new AssumeRunningAction(async () =>
        {
            (await (await _app.MyShop(_channel!)).GoToMyShop().Execute()).ShouldSucceed();
        }, this);
    }

    public IAssumeRunning Erp()
    {
        return new AssumeRunningAction(async () =>
        {
            (await _app.Erp().GoToErp().Execute()).ShouldSucceed();
        }, this);
    }

    public IAssumeRunning Tax()
    {
        return new AssumeRunningAction(async () =>
        {
            (await _app.Tax().GoToTax().Execute()).ShouldSucceed();
        }, this);
    }

    public IAssumeRunning Clock()
    {
        return new AssumeRunningAction(async () =>
        {
            (await _app.Clock().GoToClock().Execute()).ShouldSucceed();
        }, this);
    }

    public IAssumeRunning IdentityProvider()
    {
        return new AssumeRunningAction(async () =>
        {
            (await _app.IdentityProvider().GoToIdentityProvider().Execute()).ShouldSucceed();
        }, this);
    }

    private class AssumeRunningAction : IAssumeRunning
    {
        private readonly Func<Task> _action;
        private readonly IAssumeStage _assumeStage;

        public AssumeRunningAction(Func<Task> action, IAssumeStage assumeStage)
        {
            _action = action;
            _assumeStage = assumeStage;
        }

        public async Task<IAssumeStage> ShouldBeRunning()
        {
            await _action();
            return _assumeStage;
        }
    }
}
