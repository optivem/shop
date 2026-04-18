using Common;
using Driver.Adapter.External.Tax.Client;
using Driver.Port.External.Tax.Dtos;
using Driver.Port.External.Tax.Dtos.Error;
using Driver.Adapter.External.Tax.Client.Dtos;
using Driver.Adapter.External.Tax.Client.Dtos.Error;

namespace Driver.Adapter.External.Tax;

public class TaxStubDriver : BaseTaxDriver<TaxStubClient>
{
    public TaxStubDriver(string baseUrl) : base(new TaxStubClient(baseUrl))
    {
    }

    public override Task<Result<VoidValue, TaxErrorResponse>> ReturnsTaxRateAsync(ReturnsTaxRateRequest request)
    {
        var country = request.Country!;
        var taxRate = Converter.ToDecimal(request.TaxRate)!.Value;

        var response = new ExtGetCountryResponse
        {
            Id = country,
            TaxRate = taxRate,
            CountryName = country
        };

        return _client.ConfigureGetCountryAsync(response)
            .MapErrorAsync(MapError);
    }
}
