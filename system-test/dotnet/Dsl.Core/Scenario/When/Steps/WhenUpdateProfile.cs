using Dsl.Core.Scenario.When.Steps.Base;
using Dsl.Port;
using Dsl.Port.When.Steps;
using Driver.Port.Dtos;
using Dsl.Core.UseCase.UseCases;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.When.Steps;

public class WhenUpdateProfile : BaseWhen<ViewProfileResponse, ViewProfileVerification>, IWhenUpdateProfile
{
    private string? _firstName = DefaultFirstName;
    private string? _lastName = DefaultLastName;
    private string? _street = DefaultStreet;
    private string? _postalCode = DefaultPostalCode;
    private string? _city = DefaultCity;
    private string? _country = DefaultProfileCountry;

    public WhenUpdateProfile(UseCaseDsl app, ScenarioDsl scenario, Func<Task> ensureGiven)
        : base(app, scenario, ensureGiven)
    {
    }

    public WhenUpdateProfile WithFirstName(string? firstName)
    {
        _firstName = firstName;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithFirstName(string? firstName) => WithFirstName(firstName);

    public WhenUpdateProfile WithLastName(string? lastName)
    {
        _lastName = lastName;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithLastName(string? lastName) => WithLastName(lastName);

    public WhenUpdateProfile WithStreet(string? street)
    {
        _street = street;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithStreet(string? street) => WithStreet(street);

    public WhenUpdateProfile WithPostalCode(string? postalCode)
    {
        _postalCode = postalCode;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithPostalCode(string? postalCode) => WithPostalCode(postalCode);

    public WhenUpdateProfile WithCity(string? city)
    {
        _city = city;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithCity(string? city) => WithCity(city);

    public WhenUpdateProfile WithCountry(string? country)
    {
        _country = country;
        return this;
    }

    IWhenUpdateProfile IWhenUpdateProfile.WithCountry(string? country) => WithCountry(country);

    protected override async Task<ExecutionResult<ViewProfileResponse, ViewProfileVerification>> Execute(UseCaseDsl app)
    {
        var shop = await app.MyShop(ChannelMode.Dynamic, Channel);
        var result = await shop.UpdateProfile()
            .FirstName(_firstName)
            .LastName(_lastName)
            .Street(_street)
            .PostalCode(_postalCode)
            .City(_city)
            .Country(_country)
            .Execute();

        return new ExecutionResultBuilder<ViewProfileResponse, ViewProfileVerification>(result).Build();
    }
}
