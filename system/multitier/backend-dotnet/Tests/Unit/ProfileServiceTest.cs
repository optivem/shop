using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Backend.Core.Dtos;
using MyCompany.MyShop.Backend.Core.Services;
using MyCompany.MyShop.Backend.Data;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Unit;

public class ProfileServiceTest : IDisposable
{
    private static readonly CurrentUser Ana = new("sub-ana", "ana", false, "ana@example.com", "Ana", "Novak");
    private static readonly CurrentUser Bob = new("sub-bob", "bob", false, "bob@example.com", "Bob", "Smith");

    private readonly AppDbContext _dbContext;
    private readonly ProfileService _service;

    public ProfileServiceTest()
    {
        _dbContext = new AppDbContext(new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase($"test-{Guid.NewGuid()}")
            .Options);
        _service = new ProfileService(_dbContext);
    }

    public void Dispose()
    {
        _dbContext.Dispose();
        GC.SuppressFinalize(this);
    }

    [Fact]
    public async Task ViewProfile_WithoutSavedProfileTakesNamesAndEmailFromTheAccount()
    {
        var profile = await _service.ViewProfileAsync(Ana);

        Assert.Equal("Ana", profile.FirstName);
        Assert.Equal("Novak", profile.LastName);
        Assert.Equal("ana@example.com", profile.Email);
        Assert.Null(profile.Street);
        Assert.Null(profile.PostalCode);
        Assert.Null(profile.City);
        Assert.Null(profile.Country);
    }

    [Fact]
    public async Task ViewProfile_WithoutSavedProfileOrAccountNamesHasNullNames()
    {
        var profile = await _service.ViewProfileAsync(new CurrentUser("sub-x", "x", false, "x@example.com"));

        Assert.Null(profile.FirstName);
        Assert.Null(profile.LastName);
        Assert.Equal("x@example.com", profile.Email);
    }

    [Fact]
    public async Task UpdateProfile_SavesTheTrimmedValuesAndReturnsThem()
    {
        var saved = await _service.UpdateProfileAsync(Request(" Anna ", " Main St 1 ", "DE "), Ana);

        Assert.Equal("Anna", saved.FirstName);
        Assert.Equal("Main St 1", saved.Street);
        Assert.Equal("DE", saved.Country);
        Assert.Equal("ana@example.com", saved.Email);

        var viewed = await _service.ViewProfileAsync(Ana);
        Assert.Equal("Anna", viewed.FirstName);
        Assert.Equal("Novak", viewed.LastName);
        Assert.Equal("Main St 1", viewed.Street);
        Assert.Equal("10115", viewed.PostalCode);
        Assert.Equal("Berlin", viewed.City);
        Assert.Equal("DE", viewed.Country);
    }

    [Fact]
    public async Task UpdateProfile_ReplacesTheSavedProfile()
    {
        await _service.UpdateProfileAsync(Request("Ana", "Main St 1", "DE"), Ana);

        await _service.UpdateProfileAsync(Request("Ana", "1 High St", "GB"), Ana);

        var row = await _dbContext.CustomerProfiles.SingleAsync();
        Assert.Equal("sub-ana", row.Owner);
        Assert.Equal("1 High St", row.Street);
        Assert.Equal("GB", row.Country);
    }

    [Fact]
    public async Task ViewProfile_ShowsOnlyTheCallersOwnProfile()
    {
        await _service.UpdateProfileAsync(Request("Ana", "Private Lane 7", "DE"), Ana);

        var bobs = await _service.ViewProfileAsync(Bob);

        Assert.Null(bobs.Street);
        Assert.Equal("Bob", bobs.FirstName);
    }

    private static UpdateProfileRequest Request(string firstName, string street, string country) => new()
    {
        FirstName = firstName,
        LastName = "Novak",
        Street = street,
        PostalCode = "10115",
        City = "Berlin",
        Country = country
    };
}
