using Dsl.Port.Given.Steps.Base;

namespace Dsl.Port.Given.Steps;

public interface IGivenErp : IGivenStep
{
    IGivenErp Unavailable();

    IGivenErp Slow();
}
