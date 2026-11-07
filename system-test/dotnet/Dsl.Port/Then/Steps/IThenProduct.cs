using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenProduct
{
    IThenProduct HasSku(string sku);

    IThenProduct HasPrice(decimal price);

    IThenProduct HasTitle(string title);

    IThenProduct HasDescription(string description);

    IThenProduct HasCategory(string category);

    IThenProduct HasBrand(string brand);

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenProducts Products();

    IThenCountry Country(string countryAlias);

    TaskAwaiter GetAwaiter();
}
