using Dsl.Core.External.Tax.UseCases;
using Dsl.Port.Then.Steps;

namespace Dsl.Core.Scenario.Then.Steps;

public class ThenCountry : BaseThenLookup<GetTaxVerification>, IThenCountry
{
    private readonly string _countryAlias;

    public ThenCountry(UseCaseDsl app, string countryAlias, Func<Task> prelude)
        : base(app, prelude)
    {
        _countryAlias = countryAlias;
    }

    protected override async Task<GetTaxVerification> Fetch()
    {
        return (await _app.Tax().GetTaxRate().Country(_countryAlias).Execute()).ShouldSucceed();
    }

    public IThenCountry HasCountry(string country)
    {
        AddVerification(v => v.Country(country));
        return this;
    }

    public IThenCountry HasTaxRate(decimal taxRate)
    {
        AddVerification(v => v.TaxRate(taxRate));
        return this;
    }

    public IThenCountry HasTaxRateIsPositive()
    {
        AddVerification(v => v.TaxRateIsPositive());
        return this;
    }
}
