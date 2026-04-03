using Dsl.Port;
using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Optivem.Testing;
using Xunit;
using Dsl.Core.Scenario;

namespace SystemTests.Legacy.Mod09.Base;

public abstract class BaseScenarioDslTest : BaseConfigurableTest, IAsyncLifetime
{
    private UseCaseDsl _app = null!;

    public virtual async Task InitializeAsync()
    {
        await TestLock.WaitAsync();
        var configuration = LoadConfiguration();
        _app = new UseCaseDsl(configuration);
    }

    protected IScenarioDsl Scenario(Channel channel)
    {
        return new ScenarioDsl(channel, _app);
    }

    protected IScenarioDsl Scenario()
    {
        return new ScenarioDsl(null, _app);
    }

    public virtual async Task DisposeAsync()
    {
        try
        {
            if (_app != null)
                await _app.DisposeAsync();
        }
        finally
        {
            TestLock.Release();
        }
    }
}












