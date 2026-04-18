using Common;
using Driver.Adapter.Shared.Client.WireMock;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.External.Tax.Client.Dtos;
using Driver.Adapter.External.Tax.Client.Dtos.Error;

namespace Driver.Adapter.External.Tax.Client;

public class TaxStubClient : BaseTaxClient
{
    private const string TaxCountriesEndpoint = "/tax/api/countries";

    private readonly JsonWireMockClient _wireMockClient;

    public TaxStubClient(string baseUrl) : base(baseUrl)
    {
        _wireMockClient = new JsonWireMockClient(baseUrl);
    }

    public Task<Result<VoidValue, ExtTaxErrorResponse>> ConfigureGetCountryAsync(ExtGetCountryResponse response)
        => _wireMockClient.StubGetAsync($"{TaxCountriesEndpoint}/{response.Id}", HttpStatus.Ok, response)
            .MapErrorAsync(ExtTaxErrorResponse.From);
}
