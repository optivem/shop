using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Monolith.Api;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

public class ProductionConfigGuardTests
{
    private static IConfiguration Config(params (string Key, string Value)[] values) =>
        new ConfigurationBuilder()
            .AddInMemoryCollection(values.Select(v => new KeyValuePair<string, string?>(v.Key, v.Value)))
            .Build();

    [Fact]
    public void AcceptsRealValues()
    {
        var config = Config(("POSTGRES_DB_PASSWORD", "s3cret"), ("AUTH_ISSUER_URI", "https://auth.example.com/realms/shop"),
            ("AUTH_CLIENT_SECRET", "real-secret"), ("EXTERNAL_SYSTEM_MODE", "real"));

        Assert.Empty(ProductionConfigGuard.Problems(config));
    }

    [Fact]
    public void RejectsTheStubClock()
    {
        Assert.Contains("EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)",
            ProductionConfigGuard.Problems(Config(("EXTERNAL_SYSTEM_MODE", "stub"))));
    }
}
