using Dsl.Core.UseCase.UseCases;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

/// <summary>
/// Profile assertions, run after the prelude against the profile the fetch yields: either the executed
/// operation's own response, or the acting user's profile fetched afresh.
/// </summary>
public class ThenProfile : BaseThenLookup<ViewProfileVerification>, IThenProfile
{
    private readonly Func<Task<ViewProfileVerification>> _fetch;

    public ThenProfile(UseCaseDsl app, Func<Task> prelude, Func<Task<ViewProfileVerification>> fetch)
        : base(app, prelude)
    {
        _fetch = fetch;
    }

    /// <summary>The acting user's profile, fetched afresh with GET /api/account/profile.</summary>
    public static ThenProfile Refetched(UseCaseDsl app, Optivem.Testing.Channel channel, Func<Task> prelude) =>
        new(app, prelude, async () => (await (await app.MyShop(channel)).ViewProfile().Execute()).ShouldSucceed());

    protected override Task<ViewProfileVerification> Fetch() => _fetch();

    public IThenProfile HasFirstName(string expectedFirstName)
    {
        AddVerification(v => v.FirstName(expectedFirstName));
        return this;
    }

    public IThenProfile HasLastName(string expectedLastName)
    {
        AddVerification(v => v.LastName(expectedLastName));
        return this;
    }

    public IThenProfile HasEmail(string expectedEmail)
    {
        AddVerification(v => v.Email(expectedEmail));
        return this;
    }

    public IThenProfile HasStreet(string expectedStreet)
    {
        AddVerification(v => v.Street(expectedStreet));
        return this;
    }

    public IThenProfile HasPostalCode(string expectedPostalCode)
    {
        AddVerification(v => v.PostalCode(expectedPostalCode));
        return this;
    }

    public IThenProfile HasCity(string expectedCity)
    {
        AddVerification(v => v.City(expectedCity));
        return this;
    }

    public IThenProfile HasCountry(string expectedCountry)
    {
        AddVerification(v => v.Country(expectedCountry));
        return this;
    }

    public IThenProfile DoesNotHaveStreet(string street)
    {
        AddVerification(v => v.StreetIsNot(street));
        return this;
    }

    public IThenProfile HasNoSavedAddress()
    {
        AddVerification(v => v.AddressIsEmpty());
        return this;
    }

    public IThenProfile HasEmailThatCannotBeChanged()
    {
        AddVerification(v => v.EmailCannotBeChanged());
        return this;
    }

    public IThenProfile HasCountryChoices(params string[] expectedCountries)
    {
        AddVerification(v => v.CountryChoices(expectedCountries));
        return this;
    }

    public IThenProfile HasTaxNotice(string expectedNotice)
    {
        AddVerification(v => v.TaxNotice(expectedNotice));
        return this;
    }

    public IThenProfile HasMessage(string expectedMessage)
    {
        AddVerification(v => v.Message(expectedMessage));
        return this;
    }
}
