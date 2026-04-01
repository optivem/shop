using Dsl.Port.Given.Steps;
using Driver.Adapter;
using Dsl.Core.Gherkin;
using static Dsl.Core.Gherkin.GherkinDefaults;

namespace Dsl.Core.Scenario.Given;

public class GivenClock : BaseGiven, IGivenClock
{
    private string? _time;

    public GivenClock(GivenStage givenClause) : base(givenClause)
    {
        WithTime(DefaultTime);
    }

    public GivenClock WithTime(string? time)
    {
        _time = time;
        return this;
    }

    IGivenClock IGivenClock.WithTime(string? time) => WithTime(time);

    public GivenClock WithWeekday()
    {
        return WithTime(WeekdayTime);
    }

    IGivenClock IGivenClock.WithWeekday() => WithWeekday();

    public GivenClock WithWeekend()
    {
        return WithTime(WeekendTime);
    }

    IGivenClock IGivenClock.WithWeekend() => WithWeekend();

    internal override async Task Execute(UseCaseDsl app)
    {
        (await app.Clock().ReturnsTime()
            .Time(_time)
            .Execute())
            .ShouldSucceed();
    }
}


