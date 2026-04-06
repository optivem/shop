using Dsl.Port;
using Dsl.Core.Shared;
using Dsl.Core;

namespace SystemTests.TestInfrastructure.Configuration;

public abstract class BaseConfigurableTest
{
    protected virtual Environment? GetFixedEnvironment()
    {
        return null;
    }

    protected virtual ExternalSystemMode? GetFixedExternalSystemMode()
    {
        return null;
    }

    protected virtual ChannelMode? GetFixedChannelMode()
    {
        return null;
    }

    protected Dsl.Core.Configuration LoadConfiguration()
    {
        var environment = PropertyLoader.GetEnvironment(GetFixedEnvironment());
        var externalSystemMode = PropertyLoader.GetExternalSystemMode(GetFixedExternalSystemMode());
        var channelMode = PropertyLoader.GetChannelMode(GetFixedChannelMode());

        return SystemConfigurationLoader.Load(environment, externalSystemMode, channelMode);
    }
}












