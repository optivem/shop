using SystemTests.TestInfrastructure.Configuration;
using Dsl.Core;
using Xunit;

namespace SystemTests.Legacy.Mod07.Base;

public abstract class BaseSystemDslTest : BaseConfigurableTest, IAsyncLifetime
{
    protected UseCaseDsl _app = null!;

    public virtual async Task InitializeAsync()
    {
        await TestLock.WaitAsync();

        var configuration = LoadConfiguration();
        _app = new UseCaseDsl(configuration);
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











