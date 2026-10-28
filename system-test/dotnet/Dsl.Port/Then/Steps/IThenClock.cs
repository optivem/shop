using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenClock
{
    IThenClock HasTime(string time);

    IThenClock HasTime();

    IThenClock Clock();

    IThenProduct Product(string skuAlias);

    IThenCountry Country(string countryAlias);

    TaskAwaiter GetAwaiter();
}
