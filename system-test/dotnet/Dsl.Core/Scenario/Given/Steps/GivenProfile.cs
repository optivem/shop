using Dsl.Port.Given.Steps;
using Driver.Port;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.Given;

/// <summary>A profile saved before the When step, by the default customer unless another one is named.</summary>
public class GivenProfile : BaseGiven, IGivenProfile
{
    private readonly GivenStage _given;
    private string? _firstName = DefaultFirstName;
    private string? _lastName = DefaultLastName;
    private string? _street = DefaultStreet;
    private string? _postalCode = DefaultPostalCode;
    private string? _city = DefaultCity;
    private string? _country = DefaultProfileCountry;
    private string? _savedByAlias;

    public GivenProfile(GivenStage givenClause) : base(givenClause)
    {
        _given = givenClause;
    }

    public GivenProfile WithFirstName(string? firstName)
    {
        _firstName = firstName;
        return this;
    }

    IGivenProfile IGivenProfile.WithFirstName(string? firstName) => WithFirstName(firstName);

    public GivenProfile WithLastName(string? lastName)
    {
        _lastName = lastName;
        return this;
    }

    IGivenProfile IGivenProfile.WithLastName(string? lastName) => WithLastName(lastName);

    public GivenProfile WithStreet(string? street)
    {
        _street = street;
        return this;
    }

    IGivenProfile IGivenProfile.WithStreet(string? street) => WithStreet(street);

    public GivenProfile WithPostalCode(string? postalCode)
    {
        _postalCode = postalCode;
        return this;
    }

    IGivenProfile IGivenProfile.WithPostalCode(string? postalCode) => WithPostalCode(postalCode);

    public GivenProfile WithCity(string? city)
    {
        _city = city;
        return this;
    }

    IGivenProfile IGivenProfile.WithCity(string? city) => WithCity(city);

    public GivenProfile WithCountry(string? country)
    {
        _country = country;
        return this;
    }

    IGivenProfile IGivenProfile.WithCountry(string? country) => WithCountry(country);

    public GivenProfile SavedByCustomer()
    {
        _savedByAlias = null;
        return this;
    }

    IGivenProfile IGivenProfile.SavedByCustomer() => SavedByCustomer();

    public GivenProfile SavedByCustomer(string alias)
    {
        _given.RegisterCustomer(alias);
        _savedByAlias = alias;
        return this;
    }

    IGivenProfile IGivenProfile.SavedByCustomer(string alias) => SavedByCustomer(alias);

    internal bool IsSavedByDefaultCustomer => _savedByAlias == null;

    internal override async Task Execute(UseCaseDsl app)
    {
        app.ActAs(_savedByAlias == null ? GivenStage.DefaultCustomer() : _given.ResolveCustomer(_savedByAlias));
        try
        {
            var shop = await app.MyShop(Channel!);
            (await shop.UpdateProfile()
                .FirstName(_firstName)
                .LastName(_lastName)
                .Street(_street)
                .PostalCode(_postalCode)
                .City(_city)
                .Country(_country)
                .Execute())
                .ShouldSucceed();
        }
        finally
        {
            app.ActAs(UserIdentity.Default);
        }
    }
}
