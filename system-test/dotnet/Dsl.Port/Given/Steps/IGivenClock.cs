using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

public interface IGivenClock : IGivenStep
{
    IGivenClock WithTime();
    IGivenClock WithTime(string? time);
    IGivenClock WithWeekday();
    IGivenClock WithWeekend();
}
