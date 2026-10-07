using Dsl.Port;
using Microsoft.Extensions.Configuration;
using Dsl.Core.Shared;
using Dsl.Core;

namespace SystemTests.TestInfrastructure.Configuration;

public static class SystemConfigurationLoader
{
    public static Dsl.Core.Configuration Load(Environment environment, ExternalSystemMode externalSystemMode,
        ChannelMode channelMode)
    {
        var configFile = GetConfigFileName(environment, externalSystemMode);
        var configuration = LoadJsonFile(configFile);

        // Env var overrides let the cross-lang verification workflow point this
        // test assembly at a different language's SUT without touching the JSON files.
        // Suffix matches externalSystemMode so stub/real suites in one tests.yaml
        // run can each get their own URL set.
        var suffix = "_" + externalSystemMode.ToString().ToUpper();
        var shopUiBaseUrl = GetEnvVarOrDefault("MYSHOP_UI_BASE_URL" + suffix, GetValue(configuration, "MyShop:UiBaseUrl"));
        var shopApiBaseUrl = GetEnvVarOrDefault("MYSHOP_API_BASE_URL" + suffix, GetValue(configuration, "MyShop:ApiBaseUrl"));
        var erpBaseUrl = GetEnvVarOrDefault("ERP_API_BASE_URL" + suffix, GetValue(configuration, "Erp:ApiBaseUrl"));
        var taxBaseUrl = GetEnvVarOrDefault("TAX_API_BASE_URL" + suffix, GetValue(configuration, "Tax:ApiBaseUrl"));
        var clockBaseUrl = GetEnvVarOrDefault("CLOCK_API_BASE_URL" + suffix, GetValue(configuration, "Clock:ApiBaseUrl"));

        var keycloakBaseUrl = ResolveKeycloakBaseUrl(externalSystemMode);

        return new Dsl.Core.Configuration(shopUiBaseUrl, shopApiBaseUrl, erpBaseUrl, taxBaseUrl, clockBaseUrl,
            keycloakBaseUrl, externalSystemMode, channelMode);
    }

    /// <summary>
    /// Returns null when no Keycloak URL is configured; suites that need authentication must reject null.
    /// </summary>
    public static string? ResolveKeycloakBaseUrl(ExternalSystemMode externalSystemMode)
    {
        var suffix = "_" + externalSystemMode.ToString().ToUpper();
        var keycloakBaseUrl = System.Environment.GetEnvironmentVariable("KEYCLOAK_URL" + suffix);
        if (string.IsNullOrWhiteSpace(keycloakBaseUrl))
            keycloakBaseUrl = System.Environment.GetEnvironmentVariable("KEYCLOAK_URL");
        return string.IsNullOrWhiteSpace(keycloakBaseUrl) ? null : keycloakBaseUrl;
    }

    private static string GetEnvVarOrDefault(string envVarName, string fileValue)
    {
        var envValue = System.Environment.GetEnvironmentVariable(envVarName);
        return string.IsNullOrWhiteSpace(envValue) ? fileValue : envValue;
    }

    private static string GetConfigFileName(Environment environment, ExternalSystemMode externalSystemMode)
    {
        var env = environment.ToString().ToLower();
        var mode = externalSystemMode.ToString().ToLower();

        return $"appsettings.{env}.{mode}.json";
    }

    private static IConfiguration LoadJsonFile(string fileName)
    {
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile(fileName, optional: false, reloadOnChange: false)
            .Build();

        if (configuration == null)
        {
            throw new InvalidOperationException($"Configuration file not found: {fileName}");
        }

        return configuration;
    }

    private static string GetValue(IConfiguration configuration, string key)
    {
        var value = configuration[key];

        if (string.IsNullOrEmpty(value))
        {
            throw new InvalidOperationException($"Configuration value for '{key}' is missing or empty.");
        }

        return value;
    }
}












