using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenCountry
{
    IThenCountry HasCountry(string country);

    IThenCountry HasTaxRate(decimal taxRate);

    IThenCountry HasTaxRateIsPositive();

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    TaskAwaiter GetAwaiter();
}
