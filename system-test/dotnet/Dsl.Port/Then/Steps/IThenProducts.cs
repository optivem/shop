using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenProducts
{
    IThenProducts ContainsProduct(string skuAlias, decimal price);

    IThenProducts ContainsProduct(string skuAlias, string price);

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenProducts Products();

    IThenCountry Country(string countryAlias);

    TaskAwaiter GetAwaiter();
}
