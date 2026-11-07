using Dsl.Port.Then.Steps;

namespace Dsl.Port.Then;

public interface IThenStage
{
    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenProducts Products();

    IThenCountry Country(string countryAlias);
}
