using Common;

using Driver.Adapter.Shared.Client.WireMock;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.External.Erp.Client.Dtos;

using Driver.Adapter.External.Erp.Client.Dtos.Error;



namespace Driver.Adapter.External.Erp.Client;



public class ErpStubClient : BaseErpClient

{

    private const string ErpProductsEndpoint = "/erp/api/products";
    private const string ErpPromotionEndpoint = "/erp/api/promotion";
    private const string ErpApiPattern = "/erp/api/.*";
    // Far past the system's 2s outbound read timeout, so every attempt times out; also longer than all
    // retries together take, so a system without a timeout would be caught by a fail-fast assertion.
    private const int SlowResponseDelayMilliseconds = 20_000;



    private readonly JsonWireMockClient _wireMockClient;



    public ErpStubClient(string baseUrl) : base(baseUrl)

    {

        _wireMockClient = new JsonWireMockClient(baseUrl);

    }



    public new void Dispose()

    {

        base.Dispose();

        _wireMockClient?.Dispose();

    }



    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetProductAsync(ExtProductDetailsResponse response)

        => _wireMockClient.StubGetAsync($"{ErpProductsEndpoint}/{response.Id}", HttpStatus.Ok, response)

            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Asking for this product answers 404; the newest stub wins over the product stubbed before.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetProductNotFoundAsync(string sku)
        => _wireMockClient.StubGetAsync($"{ErpProductsEndpoint}/{sku}", HttpStatus.NotFound)
            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetProductsAsync(List<ExtProductDetailsResponse> response)
        => _wireMockClient.StubGetAsync(ErpProductsEndpoint, HttpStatus.Ok, response)
            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetPromotionAsync(ExtGetPromotionResponse response)

        => _wireMockClient.StubGetAsync(ErpPromotionEndpoint, HttpStatus.Ok, response)

            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Every ERP API call answers 503, outranking any product or promotion stubs.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureUnavailableAsync()

        => _wireMockClient.StubGetMatchingAsync(ErpApiPattern, HttpStatus.ServiceUnavailable)

            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Every ERP API call answers only after a delay past the system's timeout, outranking any product or promotion stubs.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureSlowAsync()
        => _wireMockClient.StubGetMatchingAsync(ErpApiPattern, HttpStatus.Ok, SlowResponseDelayMilliseconds)
            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Removes the outage stub (unavailable or slow), so the product and promotion stubs answer again.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> RemoveUnavailableAsync()
        => _wireMockClient.RemoveStubsMatchingAsync(ErpApiPattern)
            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task RemoveStubsAsync() => _wireMockClient.RemoveStubsAsync();

}





