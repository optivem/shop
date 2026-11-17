using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Backend.Api;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Unit;

public class ProductionConfigGuardTest
{
    private static IConfiguration Config(params (string Key, string Value)[] values) =>
        new ConfigurationBuilder()
            .AddInMemoryCollection(values.Select(v => new KeyValuePair<string, string?>(v.Key, v.Value)))
            .Build();

    [Fact]
    public void OnlyAppProfileProdIsTheProdProfile()
    {
        Assert.True(ProductionConfigGuard.IsProdProfile(Config(("APP_PROFILE", "prod"))));
        Assert.False(ProductionConfigGuard.IsProdProfile(Config(("APP_PROFILE", "dev"))));
        Assert.False(ProductionConfigGuard.IsProdProfile(Config()));
    }

    [Fact]
    public void RejectsTheStubClockButAcceptsRealMode()
    {
        Assert.Contains("EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)",
            ProductionConfigGuard.Problems(Config(("EXTERNAL_SYSTEM_MODE", "stub"))));
        Assert.DoesNotContain(ProductionConfigGuard.Problems(Config(("EXTERNAL_SYSTEM_MODE", "real"))),
            problem => problem.StartsWith("EXTERNAL_SYSTEM_MODE"));
    }

    [Fact]
    public void AcceptsRealValues()
    {
        var config = Config(("POSTGRES_DB_PASSWORD", "s3cret"), ("AUTH_ISSUER_URI", "https://auth.example.com/realms/shop"));

        Assert.Empty(ProductionConfigGuard.Problems(config));
    }

    [Fact]
    public void RejectsTestDefaultsAndMissingValues()
    {
        var config = Config(("POSTGRES_DB_PASSWORD", "app"));

        Assert.Equal(
            ["POSTGRES_DB_PASSWORD is missing or the test default", "AUTH_ISSUER_URI is missing or points at localhost"],
            ProductionConfigGuard.Problems(config));
    }

    [Fact]
    public void OnlyChecksUnderTheProdProfile()
    {
        ProductionConfigGuard.EnsureSafeForProduction(Config(("POSTGRES_DB_PASSWORD", "app")));

        Assert.Throws<InvalidOperationException>(() =>
            ProductionConfigGuard.EnsureSafeForProduction(Config(("APP_PROFILE", "prod"), ("POSTGRES_DB_PASSWORD", "app"))));
    }
}
