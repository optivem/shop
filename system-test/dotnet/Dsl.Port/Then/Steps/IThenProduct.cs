using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenProduct
{
    IThenProduct HasSku(string sku);

    IThenProduct HasPrice(decimal price);

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    TaskAwaiter GetAwaiter();
}
