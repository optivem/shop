using Dsl.Port.Given.Steps;
using Driver.Adapter;

namespace Dsl.Core.Scenario.Given;

/// <summary>The ERP's availability; an unavailable ERP fails every call, and a slow ERP answers every call only after the system's outbound timeout, whatever products or promotion are staged.</summary>
public class GivenErp : BaseGiven, IGivenErp
{
    private bool _unavailable;
    private bool _slow;

    public GivenErp(GivenStage givenClause) : base(givenClause)
    {
    }

    public GivenErp Unavailable()
    {
        _unavailable = true;
        return this;
    }

    IGivenErp IGivenErp.Unavailable() => Unavailable();

    public GivenErp Slow()
    {
        _slow = true;
        return this;
    }

    IGivenErp IGivenErp.Slow() => Slow();

    internal override async Task Execute(UseCaseDsl app)
    {
        if (_slow)
        {
            (await app.Erp().ReturnsSlow()
                .Execute())
                .ShouldSucceed();
        }

        if (_unavailable)
        {
            (await app.Erp().ReturnsUnavailable()
                .Execute())
                .ShouldSucceed();
        }
    }
}
