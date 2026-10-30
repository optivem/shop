using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using MyCompany.MyShop.Monolith.Data;
using Testcontainers.PostgreSql;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests.Integration;

public abstract class AbstractIntegrationTest : IAsyncLifetime
{
    private readonly PostgreSqlContainer _postgres = new PostgreSqlBuilder()
        .WithImage("postgres:16-alpine")
        .WithDatabase("app")
        .WithUsername("app")
        .WithPassword("app")
        .Build();

    protected AppDbContext DbContext { get; private set; } = null!;

    protected AppDbContext NewDbContext(params IInterceptor[] interceptors) =>
        new(new DbContextOptionsBuilder<AppDbContext>()
            .UseNpgsql(_postgres.GetConnectionString())
            .AddInterceptors(interceptors)
            .Options);

    public async Task InitializeAsync()
    {
        await _postgres.StartAsync();

        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseNpgsql(_postgres.GetConnectionString())
            .Options;

        DbContext = new AppDbContext(options);
        await DbContext.Database.EnsureCreatedAsync();
    }

    public async Task DisposeAsync()
    {
        await DbContext.DisposeAsync();
        await _postgres.DisposeAsync();
    }
}
