using Dsl.Core.External.Clock.UseCases;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

public class ThenClock : BaseThenLookup<GetTimeVerification>, IThenClock
{
    public ThenClock(UseCaseDsl app, Func<Task> prelude)
        : base(app, prelude)
    {
    }

    protected override async Task<GetTimeVerification> Fetch()
    {
        return (await _app.Clock().GetTime().Execute()).ShouldSucceed();
    }

    public IThenClock HasTime(string time)
    {
        AddVerification(v => v.Time(time));
        return this;
    }

    public IThenClock HasTime()
    {
        AddVerification(v => v.TimeIsNotNull());
        return this;
    }
}
