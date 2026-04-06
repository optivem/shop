using Dsl.Port;
using Dsl.Core.Shared;

namespace SystemTests.TestInfrastructure.Configuration;

public static class PropertyLoader
{
    public static Environment GetEnvironment(Environment? fixedEnvironment)
    {
        if (fixedEnvironment != null)
        {
            return fixedEnvironment.Value;
        }

        var environmentMode = GetRequiredEnvironmentVariable("ENVIRONMENT", "local|acceptance|qa|production");
        return Enum.Parse<Environment>(environmentMode, ignoreCase: true);
    }

    public static ExternalSystemMode GetExternalSystemMode(ExternalSystemMode? fixedExternalSystemMode)
    {
        if (fixedExternalSystemMode != null)
        {
            return fixedExternalSystemMode.Value;
        }

        var externalSystemMode = GetRequiredEnvironmentVariable("EXTERNAL_SYSTEM_MODE", "stub|real");
        return Enum.Parse<ExternalSystemMode>(externalSystemMode, ignoreCase: true);
    }

    public static ChannelMode GetChannelMode(ChannelMode? fixedChannelMode)
    {
        if (fixedChannelMode != null)
        {
            return fixedChannelMode.Value;
        }

        var value = System.Environment.GetEnvironmentVariable("CHANNEL_MODE");
        if (string.IsNullOrEmpty(value))
        {
            return ChannelMode.Dynamic;
        }
        return Enum.Parse<ChannelMode>(value, ignoreCase: true);
    }

    private static string GetRequiredEnvironmentVariable(string variableName, string allowedValues)
    {
        var value = System.Environment.GetEnvironmentVariable(variableName);

        if (string.IsNullOrEmpty(value))
        {
            throw new InvalidOperationException(
                $"Environment variable '{variableName}' is not defined. Please set {variableName}=<{allowedValues}>");
        }

        return value;
    }
}










